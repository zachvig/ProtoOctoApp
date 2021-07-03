package de.crysxd.octoapp.base.usecase

import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.dns.LocalDnsResolver
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintApiException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.cert.Certificate
import javax.inject.Inject

class TestFullNetworkStackUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    private val localDnsResolver: LocalDnsResolver,
) : UseCase<TestFullNetworkStackUseCase.Params, TestFullNetworkStackUseCase.Finding>() {

    companion object {
        private const val PING_TIMEOUT = 2000
    }

    override suspend fun doExecute(param: Params, timber: Timber.Tree): Finding = withContext(Dispatchers.IO) {
        try {
            // Parse URL
            timber.i("Testing URL syntax")
            val (baseUrl, host) = try {
                val url = Uri.parse(param.webUrl)
                url to url.host!!
            } catch (e: Exception) {
                return@withContext Finding.InvalidUrl(webUrl = param.webUrl, exception = e)
            }
            timber.i("Passed")

            // Test DNS
            timber.i("Testing DNS resolution")
            val (ip, dnsFinding) = testDns(host = host, webUrl = baseUrl.toString())
            dnsFinding?.let { return@withContext it }
            ip ?: throw RuntimeException("IP should be set if no finding was returned")
            timber.i("Passed")

            // Test reachability
            timber.i("Testing reachability")
            testReachability(host = host, ip = ip, webUrl = baseUrl.toString())?.let { return@withContext it }
            timber.i("Passed")

            // Test port open
            timber.i("Testing port access")
            val port = when {
                baseUrl.port > 0 -> baseUrl.port
                baseUrl.scheme == "http" -> 80
                baseUrl.scheme == "https" -> 443
                else -> 80
            }
            testPortOpen(host = host, ip = ip, port = port, webUrl = baseUrl.toString())?.let { return@withContext it }
            timber.i("Passed")

            // Test HTTP(S) access
            // Using the full stack here, just to be sure that the stack can also resolve the DNS
            // (should though as using same resolver)
            timber.i("Testing HTTP(S) connection")
            testHttpAccess(webUrl = param.webUrl, host = host)?.let { return@withContext it }
            timber.i("Passed")

            // Test that we actually are talking to an OctoPrint
            timber.i("Test server is OctoPrint")
            val finding = testApiKeyValid(webUrl = param.webUrl, host = host)
            when (finding) {
                is Finding.OctoPrintReady -> timber.i("Passed, OctoPrint is ready with a valid API key")
                is Finding.InvalidApiKey -> timber.i("Passed, but API key is required")
                else -> Unit
            }

            // All good!
            finding
        } catch (e: Exception) {
            Finding.UnexpectedIssue(
                webUrl = param.webUrl,
                exception = e
            )
        }
    }

    private fun testDns(host: String, webUrl: String): Pair<String?, Finding?> = try {
        localDnsResolver.resolve(host) to null
    } catch (e: UnknownHostException) {
        if (host.endsWith(".local") || host.endsWith(".home")) {
            null to Finding.LocalDnsFailure(host = host, webUrl = webUrl)
        } else {
            null to Finding.DnsFailure(host = host, webUrl = webUrl)
        }
    }

    private fun testReachability(host: String, ip: String, webUrl: String): Finding? = try {
        InetAddress.getByName(ip).isReachable(PING_TIMEOUT)
        null
    } catch (e: Exception) {
        Finding.HostNotReachable(
            host = host,
            ip = ip,
            timeoutMs = PING_TIMEOUT.toLong(),
            webUrl = webUrl
        )
    }

    private fun testPortOpen(host: String, ip: String, port: Int, webUrl: String): Finding? = try {
        val socket = Socket(ip, port)
        socket.getOutputStream()
        null
    } catch (e: Exception) {
        Finding.PortClosed(
            host = host,
            port = port,
            webUrl = webUrl
        )
    }

    private suspend fun testHttpAccess(webUrl: String, host: String): Finding? = try {
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = webUrl, apiKey = "notanapikey"))
        when (val code = octoPrint.probeConnection()) {
            in 200..299 -> null
            404 -> Finding.OctoPrintNotFound(webUrl = webUrl, host = host)
            else -> Finding.UnexpectedHttpIssue(webUrl = webUrl, host = host, exception = IOException("Unexpected HTTP response code $code"))
        }
    } catch (e: OctoPrintHttpsException) {
        Finding.HttpsNotTrusted(
            webUrl = webUrl,
            host = host,
            certificates = e.serverCertificates,
            weakHostnameVerificationRequired = e.weakHostnameVerificationRequired
        )
    } catch (e: BasicAuthRequiredException) {
        Finding.BasicAuthRequired(
            host = host,
            userRealm = e.userRealm,
            webUrl = webUrl,
        )
    } catch (e: Exception) {
        Finding.UnexpectedHttpIssue(
            webUrl = webUrl,
            exception = e,
            host = host,
        )
    }

    private suspend fun testApiKeyValid(webUrl: String, host: String): Finding = try {
        val apiKey = octoPrintRepository.findOrNull(webUrl)?.apiKey ?: ""
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = webUrl, apiKey = apiKey))
        val isApiKeyValid = octoPrint.createUserApi().getCurrentUser().isGuest.not()
        if (isApiKeyValid) {
            Finding.OctoPrintReady(webUrl = webUrl, apiKey = apiKey)
        } else {
            Finding.InvalidApiKey(webUrl = webUrl, host = host)
        }
    } catch (e: OctoPrintApiException) {
        if (e.responseCode == 404) {
            Finding.OctoPrintNotFound(webUrl = webUrl, host = host)
        } else {
            Finding.UnexpectedHttpIssue(
                webUrl = webUrl,
                exception = e,
                host = host,
            )
        }
    } catch (e: Exception) {
        Finding.UnexpectedHttpIssue(
            webUrl = webUrl,
            exception = e,
            host = host,
        )
    }

    data class Params(
        val webUrl: String
    )

    sealed class Finding {
        abstract val webUrl: String

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

        data class OctoPrintNotFound(
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

        data class OctoPrintReady(
            override val webUrl: String,
            val apiKey: String,
        ) : Finding()
    }
}