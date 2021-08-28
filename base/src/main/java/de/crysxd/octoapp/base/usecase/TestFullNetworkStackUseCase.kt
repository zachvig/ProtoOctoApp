package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.base.ui.widget.webcam.MjpegConnection
import de.crysxd.octoapp.base.ui.widget.webcam.MjpegConnection2
import de.crysxd.octoapp.octoprint.exceptions.*
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.octoprint.models.socket.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
            if (param.webUrl.isBlank()) {
                return@withContext Finding.EmptyUrl(webUrl = "")
            }
            val (baseUrl, host) = try {
                val url = Uri.parse(param.webUrl)
                url to url.host!!
            } catch (e: Exception) {
                return@withContext Finding.InvalidUrl(webUrl = param.webUrl, exception = e)
            }
            timber.i("Passed")

            // Test DNS
            timber.i("Testing DNS resolution")
            val (ip, dnsFinding) = testDns(host = host, webUrl = baseUrl.toString(), timber = timber)
            dnsFinding?.let { return@withContext it }
            ip ?: throw RuntimeException("IP should be set if no finding was returned")
            timber.i("Passed")

            // Test reachability
            timber.i("Testing reachability")
            testReachability(host = host, ip = ip, webUrl = baseUrl.toString(), timber = timber)?.let { return@withContext it }
            timber.i("Passed")

            // Test port open
            timber.i("Testing port access")
            val port = when {
                baseUrl.port > 0 -> baseUrl.port
                baseUrl.scheme == "http" -> 80
                baseUrl.scheme == "https" -> 443
                else -> 80
            }
            testPortOpen(host = host, ip = ip, port = port, webUrl = baseUrl.toString(), timber = timber)?.let { return@withContext it }
            timber.i("Passed")

            when (param) {
                is Target.OctoPrint -> testOctoPrint(timber, param, host)
                is Target.Webcam -> testWebcam(timber, param, host)
            }
        } catch (e: Exception) {
            Finding.UnexpectedIssue(
                webUrl = param.webUrl,
                exception = e
            )
        }
    }

    private suspend fun testOctoPrint(timber: Timber.Tree, target: Target.OctoPrint, host: String): Finding {
        // Test HTTP(S) access
        // Using the full stack here, just to be sure that the stack can also resolve the DNS
        // (should though as using same resolver)
        timber.i("Testing HTTP(S) connection")
        testHttpAccess(webUrl = target.webUrl, host = host, timber = timber)?.let { return it }
        timber.i("Passed")

        // Test that we actually are talking to an OctoPrint
        timber.i("Testing API key")
        testApiKeyValid(webUrl = target.webUrl, host = host, apiKey = target.apiKey, timber = timber)?.let { return it }
        timber.i("Passed.")

        // Test the websocket
        timber.i("Test web socket is working")
        testWebSocket(webUrl = target.webUrl, apiKey = target.apiKey, host = host, timber = timber)?.let { return it }
        timber.i("Passed")

        return Finding.OctoPrintReady(webUrl = target.webUrl, apiKey = target.apiKey)
    }

    private suspend fun testWebcam(timber: Timber.Tree, target: Target.Webcam, host: String) = try {
        withTimeoutOrNull(6000) {
            timber.i("Test webcam")
            var startTime: Long? = null
            val frames = 30
            val frame = MjpegConnection2(
                streamUrl = target.webUrl,
                authHeader = target.webcamSettings.authHeader,
                name = "test",
                throwExceptions = true
            ).load().mapNotNull { it as? MjpegConnection.MjpegSnapshot.Frame }.onEach {
                startTime = startTime ?: System.currentTimeMillis()
            }.take(frames).toList().last()
            val endTime = System.currentTimeMillis()
            val fps = 1000 / ((endTime - (startTime ?: 0)) / frames.toFloat())
            timber.i("Passed (%.2f FPS)", fps)
            Finding.WebcamReady(target.webUrl, fps, frame.frame)
        } ?: Finding.NoImage(webUrl = target.webUrl, host = host)
    } catch (e: OctoPrintApiException) {
        timber.w(e)
        when (e.responseCode) {
            404 -> Finding.NotFound(
                host = host,
                webUrl = target.webUrl
            )

            else -> Finding.UnexpectedHttpIssue(
                webUrl = target.webUrl,
                exception = e,
                host = host
            )
        }
    } catch (e: BasicAuthRequiredException) {
        timber.w(e)
        Finding.BasicAuthRequired(
            host = host,
            userRealm = e.userRealm,
            webUrl = target.webUrl,
        )
    } catch (e: IOException) {
        timber.w(e)
        if (e.message?.contains("Connection broken", ignoreCase = true) == true) {
            Finding.NoImage(webUrl = target.webUrl, host = host)
        } else {
            Finding.UnexpectedHttpIssue(webUrl = target.webUrl, host = host, exception = e)
        }
    } catch (e: Exception) {
        Finding.UnexpectedIssue(target.webUrl, e)
    }

    private fun testDns(host: String, webUrl: String, timber: Timber.Tree): Pair<String?, Finding?> = try {
        localDnsResolver.lookup(host).first().hostAddress to null
    } catch (e: UnknownHostException) {
        timber.w(e)
        if (host.endsWith(".local") || host.endsWith(".home") || host.endsWith(".lan")) {
            null to Finding.LocalDnsFailure(host = host, webUrl = webUrl)
        } else {
            null to Finding.DnsFailure(host = host, webUrl = webUrl)
        }
    }

    private fun testReachability(host: String, ip: String, webUrl: String, timber: Timber.Tree): Finding? = try {
        if (host.endsWith("octoeverywhere.com")) {
            timber.i("Can't ping octoeverywhere.com, skipping test")
        } else {
            require(InetAddress.getByName(ip).isReachable(PING_TIMEOUT)) { IOException("Unable to reach $host") }
        }
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

    private fun testPortOpen(host: String, ip: String, port: Int, webUrl: String, timber: Timber.Tree): Finding? = try {
        val socket = Socket(ip, port)
        socket.soTimeout = SOCKET_TIMEOUT
        socket.getOutputStream()
        null
    } catch (e: Exception) {
        timber.w(e)
        Finding.PortClosed(
            host = host,
            port = port,
            webUrl = webUrl
        )
    }

    private suspend fun testHttpAccess(webUrl: String, host: String, timber: Timber.Tree): Finding? = try {
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = webUrl, apiKey = "notanapikey"))
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

    private suspend fun testApiKeyValid(webUrl: String, host: String, apiKey: String, timber: Timber.Tree): Finding? = try {
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = webUrl, apiKey = apiKey))
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

    private suspend fun testWebSocket(webUrl: String, apiKey: String, host: String, timber: Timber.Tree) = try {
        withTimeout(3000) {
            val instance = OctoPrintInstanceInformationV2(webUrl = webUrl, apiKey = apiKey)
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

    data class Params(
        val webUrl: String
    )

    sealed class Target(open val webUrl: String) {
        data class Webcam(val webcamSettings: WebcamSettings) : Target(webcamSettings.streamUrl ?: "")
        data class OctoPrint(override val webUrl: String, val apiKey: String) : Target(webUrl)
    }

    sealed class Finding {
        abstract val webUrl: String

        data class EmptyUrl(
            override val webUrl: String,
        ) : Finding()

        data class InvalidUrl(
            override val webUrl: String,
            val exception: Exception,
        ) : Finding()

        data class LocalDnsFailure(
            override val webUrl: String,
            val host: String
        ) : Finding()

        data class DnsFailure(
            override val webUrl: String,
            val host: String
        ) : Finding()

        data class HostNotReachable(
            override val webUrl: String,
            val host: String,
            val ip: String,
            val timeoutMs: Long
        ) : Finding()

        data class PortClosed(
            override val webUrl: String,
            val host: String,
            val port: Int
        ) : Finding()

        data class BasicAuthRequired(
            override val webUrl: String,
            val host: String,
            val userRealm: String
        ) : Finding()

        data class HttpsNotTrusted(
            override val webUrl: String,
            val host: String,
            val certificates: List<Certificate>,
            val weakHostnameVerificationRequired: Boolean,
        ) : Finding()

        data class NotFound(
            override val webUrl: String,
            val host: String,
        ) : Finding()

        data class UnexpectedHttpIssue(
            override val webUrl: String,
            val host: String,
            val exception: Throwable
        ) : Finding()

        data class ServerIsNotOctoPrint(
            override val webUrl: String,
            val host: String,
        ) : Finding()

        data class InvalidApiKey(
            override val webUrl: String,
            val host: String,
        ) : Finding()

        data class UnexpectedIssue(
            override val webUrl: String,
            val exception: Throwable
        ) : Finding()

        data class WebSocketUpgradeFailed(
            override val webUrl: String,
            val host: String,
            val webSocketUrl: String,
            val responseCode: Int,
        ) : Finding()

        data class NoImage(
            override val webUrl: String,
            val host: String,
        ) : Finding()

        data class OctoPrintReady(
            override val webUrl: String,
            val apiKey: String,
        ) : Finding()

        data class WebcamReady(
            override val webUrl: String,
            val fps: Float,
            val image: Bitmap
        ) : Finding()
    }
}