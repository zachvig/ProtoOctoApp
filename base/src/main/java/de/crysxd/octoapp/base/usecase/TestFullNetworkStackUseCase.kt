package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.base.ui.widget.webcam.MjpegConnection
import de.crysxd.octoapp.base.ui.widget.webcam.MjpegConnection2
import de.crysxd.octoapp.octoprint.exceptions.*
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.octoprint.models.socket.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.cert.Certificate
import java.util.concurrent.TimeoutException
import javax.inject.Inject

class TestFullNetworkStackUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val localDnsResolver: LocalDnsResolver,
) : UseCase<TestFullNetworkStackUseCase.Target, TestFullNetworkStackUseCase.Finding>() {

    companion object {
        private const val PING_TIMEOUT = 2000
        private const val SOCKET_TIMEOUT = 3000
    }

    override suspend fun doExecute(param: Target, timber: Timber.Tree): Finding = withContext(Dispatchers.IO) {
        try {
            // Parse URL
            timber.i("Testing URL syntax")
            if (param.webUrl.isNullOrBlank()) {
                return@withContext Finding.EmptyUrl()
            }
            val (baseUrl, host) = try {
                val url = param.webUrl!!.toHttpUrl()
                url to url.host
            } catch (e: Exception) {
                return@withContext Finding.InvalidUrl(input = param.webUrl ?: "", exception = e)
            }
            timber.i("Passed")

            // Test DNS
            timber.i("Testing DNS resolution")
            val (ip, dnsFinding) = testDns(host = host, webUrl = baseUrl, timber = timber)
            dnsFinding?.let { return@withContext it }
            ip ?: throw RuntimeException("IP should be set if no finding was returned")
            timber.i("Passed")

            // Test reachability
            timber.i("Testing reachability")
            val reachable = testReachability(host = host, ip = ip, webUrl = baseUrl, timber = timber)
            timber.i(if (reachable == null) "Passed" else "Failed")

            // Test port open
            timber.i("Testing port access")
            val portOpen = testPortOpen(host = host, ip = ip, webUrl = baseUrl, timber = timber)
            timber.i(if (portOpen == null) "Passed" else "Failed")

            // Return reachability issue or port open issue if reachable
            // This setup will ignore reachability issues in case the port is open
            // Some servers can't be pinged, this solves the issue
            portOpen?.let { return@withContext reachable ?: portOpen }

            when (param) {
                is Target.OctoPrint -> testOctoPrint(timber, baseUrl, param, host)
                is Target.Webcam -> testWebcam(timber, baseUrl, host)
            }
        } catch (e: Exception) {
            Finding.UnexpectedIssue(
                webUrl = param.webUrl?.toHttpUrlOrNull(),
                exception = e
            )
        }
    }

    private suspend fun testOctoPrint(timber: Timber.Tree, webUrl: HttpUrl, target: Target.OctoPrint, host: String): Finding {
        // Test HTTP(S) access
        // Using the full stack here, just to be sure that the stack can also resolve the DNS
        // (should though as using same resolver)
        timber.i("Testing HTTP(S) connection")
        testHttpAccess(webUrl = webUrl, host = host, timber = timber)?.let { return it }
        timber.i("Passed")

        // Test that we actually are talking to an OctoPrint
        timber.i("Testing API key")
        testApiKeyValid(webUrl = webUrl, host = host, apiKey = target.apiKey, timber = timber)?.let { return it }
        timber.i("Passed.")

        // Test the websocket
        timber.i("Test web socket is working")
        testWebSocket(webUrl = webUrl, apiKey = target.apiKey, host = host, timber = timber)?.let { return it }
        timber.i("Passed")

        return Finding.OctoPrintReady(webUrl = webUrl, apiKey = target.apiKey)
    }

    private suspend fun testWebcam(timber: Timber.Tree, webcamUrl: HttpUrl, host: String) = try {
        withTimeoutOrNull(6000) {
            timber.i("Test webcam")
            var startTime: Long? = null
            val frames = 30
            val frame = MjpegConnection2(
                streamUrl = webcamUrl,
                name = "test",
                throwExceptions = true
            ).load().mapNotNull { it as? MjpegConnection.MjpegSnapshot.Frame }.onEach {
                startTime = startTime ?: System.currentTimeMillis()
            }.take(frames).toList().last()
            val endTime = System.currentTimeMillis()
            val fps = 1000 / ((endTime - (startTime ?: 0)) / frames.toFloat())
            timber.i("Passed (%.2f FPS)", fps)
            Finding.WebcamReady(webcamUrl, fps, frame.frame)
        } ?: Finding.NoImage(webUrl = webcamUrl, host = host)
    } catch (e: OctoPrintApiException) {
        timber.w(e)
        when (e.responseCode) {
            404 -> Finding.NotFound(
                host = host,
                webUrl = webcamUrl
            )

            else -> Finding.UnexpectedHttpIssue(
                webUrl = webcamUrl,
                exception = e,
                host = host
            )
        }
    } catch (e: BasicAuthRequiredException) {
        timber.w(e)
        Finding.BasicAuthRequired(
            host = host,
            userRealm = e.userRealm,
            webUrl = webcamUrl,
        )
    } catch (e: IOException) {
        timber.w(e)
        if (e.message?.contains("Connection broken", ignoreCase = true) == true) {
            Finding.NoImage(webUrl = webcamUrl, host = host)
        } else {
            Finding.UnexpectedHttpIssue(webUrl = webcamUrl, host = host, exception = e)
        }
    } catch (e: Exception) {
        Finding.UnexpectedIssue(webcamUrl, e)
    }

    private fun testDns(host: String, webUrl: HttpUrl, timber: Timber.Tree): Pair<String?, Finding?> = try {
        localDnsResolver.lookup(host).first().hostAddress to null
    } catch (e: UnknownHostException) {
        timber.w(e)
        if (host.endsWith(".local") || host.endsWith(".home") || host.endsWith(".lan")) {
            null to Finding.LocalDnsFailure(host = host, webUrl = webUrl)
        } else {
            null to Finding.DnsFailure(host = host, webUrl = webUrl)
        }
    }

    private fun testReachability(host: String, ip: String, webUrl: HttpUrl, timber: Timber.Tree): Finding? = try {
        require(InetAddress.getByName(ip).isReachable(PING_TIMEOUT)) { IOException("Unable to reach $host") }
        null
    } catch (e: Exception) {
        timber.w(e)
        Finding.HostNotReachable(
            host = host,
            ip = ip,
            timeoutMs = PING_TIMEOUT.toLong(),
            webUrl = webUrl
        )
    }

    private fun testPortOpen(host: String, ip: String, webUrl: HttpUrl, timber: Timber.Tree): Finding? = try {
        val socket = Socket(ip, webUrl.port)
        socket.soTimeout = SOCKET_TIMEOUT
        socket.getOutputStream()
        null
    } catch (e: Exception) {
        timber.w(e)
        Finding.PortClosed(
            host = host,
            port = webUrl.port,
            webUrl = webUrl
        )
    }

    private suspend fun testHttpAccess(webUrl: HttpUrl, host: String, timber: Timber.Tree): Finding? = try {
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV3(id = "adhoc", webUrl = webUrl, apiKey = "notanapikey"))
        when (val code = octoPrint.probeConnection()) {
            in 200..299 -> null
            404 -> Finding.NotFound(webUrl = webUrl, host = host)
            else -> Finding.UnexpectedHttpIssue(webUrl = webUrl, host = host, exception = IOException("Unexpected HTTP response code $code"))
        }
    } catch (e: OctoPrintHttpsException) {
        timber.w(e)
        Finding.HttpsNotTrusted(
            webUrl = webUrl,
            host = host,
            certificates = e.serverCertificates,
            weakHostnameVerificationRequired = e.weakHostnameVerificationRequired
        )
    } catch (e: BasicAuthRequiredException) {
        timber.w(e)
        Finding.BasicAuthRequired(
            host = host,
            userRealm = e.userRealm,
            webUrl = webUrl,
        )
    } catch (e: Exception) {
        timber.w(e)
        Finding.UnexpectedHttpIssue(
            webUrl = webUrl,
            exception = e,
            host = host,
        )
    }

    private suspend fun testApiKeyValid(webUrl: HttpUrl, host: String, apiKey: String, timber: Timber.Tree): Finding? = try {
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV3(id = "adhoc", webUrl = webUrl, apiKey = apiKey))
        val isApiKeyValid = octoPrint.createUserApi().getCurrentUser().isGuest.not()
        if (isApiKeyValid) {
            null
        } else {
            Finding.InvalidApiKey(webUrl = webUrl, host = host)
        }
    } catch (e: OctoPrintApiException) {
        timber.w(e)
        if (e.responseCode == 404) {
            Finding.NotFound(webUrl = webUrl, host = host)
        } else {
            Finding.UnexpectedHttpIssue(
                webUrl = webUrl,
                exception = e,
                host = host,
            )
        }
    } catch (e: Exception) {
        timber.w(e)
        Finding.UnexpectedHttpIssue(
            webUrl = webUrl,
            exception = e,
            host = host,
        )
    }

    private suspend fun testWebSocket(webUrl: HttpUrl, apiKey: String, host: String, timber: Timber.Tree) = try {
        withTimeout(3000) {
            val instance = OctoPrintInstanceInformationV3(id = "adhoc", webUrl = webUrl, apiKey = apiKey)
            when (val event = octoPrintProvider.createAdHocOctoPrint(instance).getEventWebSocket().eventFlow("test").first()) {
                is Event.Connected -> null
                is Event.Disconnected -> when (event.exception) {
                    is WebSocketUpgradeFailedException -> Finding.WebSocketUpgradeFailed(
                        webUrl = webUrl,
                        host = host,
                        webSocketUrl = (event.exception as WebSocketUpgradeFailedException).webSocketUrl.toString(),
                        responseCode = (event.exception as WebSocketUpgradeFailedException).responseCode
                    )
                    else -> Finding.UnexpectedIssue(webUrl = webUrl, exception = event.exception ?: RuntimeException("Unknown issue"))
                }
                else -> Finding.UnexpectedIssue(webUrl = webUrl, exception = RuntimeException("Unknown issue (2)"))
            }
        }
    } catch (e: TimeoutCancellationException) {
        timber.w(e)
        Finding.UnexpectedIssue(webUrl = webUrl, TimeoutException("Web socket test timed out"))
    }

    sealed class Target(open val webUrl: String?) {
        // URL is a String here because it might come directly from user input. This way we can also test URL syntax
        data class Webcam(val webcamSettings: WebcamSettings) : Target(webcamSettings.absoluteStreamUrl?.toString())
        data class OctoPrint(override val webUrl: String, val apiKey: String) : Target(webUrl)
    }

    sealed class Finding {
        abstract val webUrl: HttpUrl?

        data class EmptyUrl(
            override val webUrl: HttpUrl? = null,
        ) : Finding()

        data class InvalidUrl(
            override val webUrl: HttpUrl? = null,
            val input: String,
            val exception: Exception,
        ) : Finding()

        data class LocalDnsFailure(
            override val webUrl: HttpUrl,
            val host: String
        ) : Finding()

        data class DnsFailure(
            override val webUrl: HttpUrl,
            val host: String
        ) : Finding()

        data class HostNotReachable(
            override val webUrl: HttpUrl,
            val host: String,
            val ip: String,
            val timeoutMs: Long
        ) : Finding()

        data class PortClosed(
            override val webUrl: HttpUrl,
            val host: String,
            val port: Int
        ) : Finding()

        data class BasicAuthRequired(
            override val webUrl: HttpUrl,
            val host: String,
            val userRealm: String
        ) : Finding()

        data class HttpsNotTrusted(
            override val webUrl: HttpUrl,
            val host: String,
            val certificates: List<Certificate>,
            val weakHostnameVerificationRequired: Boolean,
        ) : Finding()

        data class NotFound(
            override val webUrl: HttpUrl,
            val host: String,
        ) : Finding()

        data class UnexpectedHttpIssue(
            override val webUrl: HttpUrl,
            val host: String,
            val exception: Throwable
        ) : Finding()

        data class ServerIsNotOctoPrint(
            override val webUrl: HttpUrl,
            val host: String,
        ) : Finding()

        data class InvalidApiKey(
            override val webUrl: HttpUrl,
            val host: String,
        ) : Finding()

        data class UnexpectedIssue(
            override val webUrl: HttpUrl?,
            val exception: Throwable
        ) : Finding()

        data class WebSocketUpgradeFailed(
            override val webUrl: HttpUrl,
            val host: String,
            val webSocketUrl: String,
            val responseCode: Int,
        ) : Finding()

        data class NoImage(
            override val webUrl: HttpUrl,
            val host: String,
        ) : Finding()

        data class OctoPrintReady(
            override val webUrl: HttpUrl,
            val apiKey: String,
        ) : Finding()

        data class WebcamReady(
            override val webUrl: HttpUrl,
            val fps: Float,
            val image: Bitmap
        ) : Finding()
    }
}