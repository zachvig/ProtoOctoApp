package de.crysxd.octoapp.signin.troubleshoot

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL

private const val MIN_CHECK_DURATION_MS = 3000L
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
                        troubleShootingResult.postValue(TroubleShootingResult.Running(1, 4, "Trying to resolve host..."))
                        runDnsTest(context, baseUrl)
                    } ?: runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(2, 4, "Trying to ping host..."))
                        runHostReachableTest(context, baseUrl)
                    } ?: runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(3, 4, "Trying to connect to port..."))
                        runPortOpenTest(context, baseUrl)
                    } ?: runAtLeast {
                        troubleShootingResult.postValue(TroubleShootingResult.Running(4, 4, "Trying to connect to OctoPrint..."))
                        runConnectionTest(context, baseUrl)
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


    private fun runDnsTest(context: Context, baseUrl: Uri) = try {
        InetAddress.getByName(baseUrl.host)
        null
    } catch (e: Exception) {
        Timber.e(e)
        TroubleShootingResult.Failure(
            text = "Can't resolve <b>${baseUrl.host}</b>",
            suggestions = listOf(
                "Check <b>${baseUrl.host}</b> is correct",
                "Check your WiFi is connected and if OctoPrint is on the local network",
                "Check you entered the correct host",
                "Check for misspelled IP addresses",
                "Check your DNS settings",
            )
        )
    }

    private fun runHostReachableTest(context: Context, baseUrl: Uri): TroubleShootingResult.Failure? {
        val host = baseUrl.host
        val reachable = try {
            val address = InetAddress.getByName(host)
            address.isReachable(TIMEOUT_MS)
        } catch (e: Exception) {
            Timber.e(e)
            false
        }

        return if (!reachable) {
            TroubleShootingResult.Failure(
                text = "Host <b>$host</b> is not reachable",
                suggestions = listOf(
                    "Check <b>$host</b> is correct",
                    "Check your WiFi is connected and if OctoPrint is on the local network",
                    "Check your OctoPrint is turned on",
                    "Check you entered the correct host"
                )
            )
        } else {
            null
        }
    }

    private fun runPortOpenTest(context: Context, baseUrl: Uri): TroubleShootingResult.Failure? {
        val port = when {
            baseUrl.port > 0 -> baseUrl.port
            baseUrl.scheme == "http" -> 80
            baseUrl.scheme == "https" -> 443
            else -> 80
        }

        return try {
            val socket = Socket(baseUrl.host, port)
            socket.getOutputStream()
            null
        } catch (e: Exception) {
            Timber.e(e)
            TroubleShootingResult.Failure(
                text = "Can connect to <b>${baseUrl.host}</b> but not to port <b>$port</b>",
                suggestions = listOf(
                    "Make sure <b>$port</b> is the correct port",
                    "If the port is not specified explicitly, 80 will be used for HTTP and 443 for HTTPS",
                )
            )
        }
    }

    private fun runConnectionTest(context: Context, baseUrl: Uri) = try {
        val connection = URL(baseUrl.toString()).openConnection() as HttpURLConnection
        connection.connect()
        val code = connection.responseCode
        if (code == 200) {
            null
        } else {
            TroubleShootingResult.Failure(
                text = "Can connect to <b>$baseUrl</b> but received <b>$code</b> instead of <b>200</b>",
                suggestions = listOf(
                    "Check you provided the correct web URL",
                    "Check any (reverse) proxy servers are correctly configured",
                    "Check your OctoPrint logs for errors",
                ),
                offerSupport = true
            )
        }
    } catch (e: Exception) {
        TroubleShootingResult.Failure(
            text = "Can't connect to <b>${baseUrl}</b> because of an error",
            suggestions = listOf(
                "Check you provided the correct web URL",
                "The host and port are reachable but the HTTP(s) connection failed because of: ${e.message}",
                "Check any (reverse) proxy servers are correctly configured",
                "Check your OctoPrint logs for errors",
            ),
            offerSupport = true
        )
    }
}