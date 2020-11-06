package de.crysxd.octoapp.signin.troubleshoot

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.signin.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL

private const val MIN_CHECK_DURATION_MS = 2000L
private const val TIMEOUT_MS = 5000

class TroubleShootViewModel : ViewModel() {

    private val troubleShootingResult = MutableLiveData<TroubleShootingResult>()

    fun runTest(context: Context, baseUrl: Uri, apiKey: String): LiveData<TroubleShootingResult> {
        if (troubleShootingResult.value !is TroubleShootingResult.Running) {
            troubleShootingResult.postValue(TroubleShootingResult.Running(0, 4, ""))

            viewModelScope.launch(Dispatchers.IO) {
                Timber.i("Running checks")
                Timber.i("Base URL: $baseUrl")
                Timber.i("API key: $apiKey")

                val result =
                    runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(1, 4, context.getString(R.string.step_1_description)))
                        runDnsTest(baseUrl)
                    } ?: runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(2, 4, context.getString(R.string.step_2_description)))
                        runHostReachableTest(baseUrl)
                    } ?: runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(3, 4, context.getString(R.string.step_3_description)))
                        runPortOpenTest(baseUrl)
                    } ?: runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(4, 4, context.getString(R.string.step_4_description)))
                        runConnectionTest(baseUrl)
                    } ?: TroubleShootingResult.Success

                troubleShootingResult.postValue(result)
            }
        }

        return troubleShootingResult
    }

    private suspend fun runAtLeast(durationMs: Long = MIN_CHECK_DURATION_MS, block: suspend () -> TroubleShootingResult.Failure?): TroubleShootingResult.Failure? {
        val start = System.currentTimeMillis()
        val b = block()
        delay((durationMs - (System.currentTimeMillis() - start)).coerceAtLeast(0L))
        return b
    }


    private fun runDnsTest(baseUrl: Uri) = try {
        Timber.i("Check 1: Resolving Host")
        Timber.i("Host: ${baseUrl.host}")
        InetAddress.getByName(baseUrl.host)
        Timber.i("Passed")
        null
    } catch (e: Exception) {
        Timber.e(e)
        Firebase.analytics.logEvent("troubleshoot_failure_dns", Bundle.EMPTY)
        TroubleShootingResult.Failure(
            title = "Can't resolve <b>${baseUrl.host}</b>",
            description = "This indicates a configuration issue. The phone can't resolve a IP address for <b>${baseUrl.host}</b>, this is not influenced by any OctoPrint settings or the API key.",
            exception = e,
            suggestions = listOf(
                "Check <b>${baseUrl.host}</b> is correct",
                "Check your WiFi is connected and if OctoPrint is on the local network",
                "Check you entered the correct host",
                "Check for misspelled IP addresses",
                "Check your DNS settings",
                "Try to open <a href=\"$baseUrl\">$baseUrl</a> in your phone's browser, OctoPrint should open",
            )
        )
    }

    private fun runHostReachableTest(baseUrl: Uri): TroubleShootingResult.Failure? {
        Timber.i("Check 2: Pinging Host")

        val host = baseUrl.host
        val reachable = try {
            val address = InetAddress.getByName(host)
            address.isReachable(TIMEOUT_MS)
        } catch (e: Exception) {
            Timber.e(e)
            false
        }

        Timber.i("Host: $host")

        return if (!reachable) {
            Firebase.analytics.logEvent("troubleshoot_failure_host", Bundle.EMPTY)
            TroubleShootingResult.Failure(
                title = "Host <b>$host</b> is not reachable",
                description = "This indicates a configuration issue. The phone can't reach <b>${baseUrl.host}</b>, this is not influenced by any OctoPrint settings or the API key.",
                suggestions = listOf(
                    "Check <b>$host</b> is correct",
                    "Check your <b>WiFi is connected</b> and if OctoPrint is on the local network",
                    "Check your OctoPrint is <b>turned on</b>",
                    "Check you entered the correct host",
                    "Try to open <a href=\"$baseUrl\">$baseUrl</a> in your phone's browser, OctoPrint should open",
                )
            )
        } else {
            Timber.i("Passed")
            null
        }
    }

    private fun runPortOpenTest(baseUrl: Uri): TroubleShootingResult.Failure? {
        Timber.i("Check 3: Connecting to Port")

        val port = when {
            baseUrl.port > 0 -> baseUrl.port
            baseUrl.scheme == "http" -> 80
            baseUrl.scheme == "https" -> 443
            else -> 80
        }

        Timber.i("Port: $port")

        return try {
            val socket = Socket(baseUrl.host, port)
            socket.getOutputStream()
            Timber.i("Passed")
            null
        } catch (e: Exception) {
            Timber.e(e)
            Firebase.analytics.logEvent("troubleshoot_failure_port", Bundle.EMPTY)
            TroubleShootingResult.Failure(
                title = "Can connect to <b>${baseUrl.host}</b> but not to port <b>$port</b>",
                description = "The phone can connect to the host, but the port <b>$port</b> does not accept incoming connections.",
                suggestions = listOf(
                    "Make sure <b>$port</b> is the correct port",
                    "If the port is not specified explicitly, 80 will be used for HTTP and 443 for HTTPS",
                    "Try to open <a href=\"$baseUrl\">$baseUrl</a> in your phone's browser, OctoPrint should open",
                ),
                exception = e
            )
        }
    }

    private fun runConnectionTest(baseUrl: Uri) = try {
        Timber.i("Check 4: HTTP connection")

        val connection = URL(baseUrl.toString()).openConnection() as HttpURLConnection
        connection.connect()
        val code = connection.responseCode
        if (code == 200) {
            Timber.i("Passed")
            null
        } else {
            Firebase.analytics.logEvent("troubleshoot_failure_http_1", Bundle.EMPTY)
            TroubleShootingResult.Failure(
                title = "Can connect to <b>$baseUrl</b> but received <b>$code</b> instead of <b>200</b>",
                description = "The app was able to establish a connection to a server, but the server did not respond as expected.",
                suggestions = listOf(
                    "Try to open <a href=\"$baseUrl\">$baseUrl</a> in your phone's browser, OctoPrint should open",
                    "Check you provided the correct web URL, especially the <b>correct path</b> to OctoPrint if you use a reverse proxy",
                    "Check any (reverse) proxy servers are correctly configured",
                    "Check your OctoPrint logs for errors",
                ),
                offerSupport = true
            )
        }
    } catch (e: Exception) {
        Firebase.analytics.logEvent("troubleshoot_failure_http_2", Bundle.EMPTY)
        TroubleShootingResult.Failure(
            title = "Can't connect to <b>${baseUrl}</b> because of an error",
            description = "A unexpected error occured while trying to connect to the server (${e.message})",
            suggestions = listOf(
                "Try to open <a href=\"$baseUrl\">$baseUrl</a> in your phone's browser, OctoPrint should open",
                "Check you provided the correct web URL",
                "Check any (reverse) proxy servers are correctly configured",
                "Check your OctoPrint logs for errors",
            ),
            exception = e,
            offerSupport = true
        )
    }
}