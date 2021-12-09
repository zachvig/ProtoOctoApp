package de.crysxd.octoapp.base.network

import android.content.Context
import android.net.wifi.WifiManager
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.octoprint.UPNP_ADDRESS_PREFIX
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.regex.Pattern

class OctoPrintUpnpDiscovery(
    context: Context,
    private val tag: String
) {
    companion object {
        private const val DISCOVER_TIMEOUT = 3000L
        private const val SOCKET_TIMEOUT = 500
        private const val PORT = 1900
        private const val ADDRESS = "239.255.255.250"
        private const val LINE_END = "\r\n"
        private const val QUERY = "M-SEARCH * HTTP/1.1" + LINE_END +
                "HOST: 239.255.255.250:1900" + LINE_END +
                "MAN: \"ssdp:discover\"" + LINE_END +
                "MX: 1" + LINE_END +
                "ST: ssdp:all" + LINE_END +
                LINE_END;
    }

    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val uuidPattern = Pattern.compile("[uU][sS][nN]:.*[uU][uU][iI][dD]:([\\-0-9a-zA-Z]{36})")
    private val Timber get() = timber.log.Timber.tag("OctoPrintUpnpDiscovery/$tag")

    suspend fun discover(callback: (Service) -> Unit) = withContext(Dispatchers.IO) {
        val lock = wifi.createMulticastLock("OctoPrintUpnpDiscovery")

        try {
            lock.acquire()
            discoverWithMulticastLock(callback)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            lock.release()
        }
    }

    private suspend fun discoverWithMulticastLock(callback: (Service) -> Unit) = withContext(Dispatchers.IO) {
        Timber.i("Opening port $PORT for UPnP, searching for ${DISCOVER_TIMEOUT}ms")
        val start = System.currentTimeMillis()
        DatagramSocket(PORT).use { socket ->
            try {
                val job = SupervisorJob()
                val cancelJob = launch(job) {
                    delay(DISCOVER_TIMEOUT)
                    Timber.w("Force closing socket")
                    socket.close()
                }

                val discoverJob = launch(job) {
                    socket.reuseAddress = true
                    val group = InetAddress.getByName(ADDRESS)
                    val queryBytes = QUERY.toByteArray()
                    val datagramPacketRequest = DatagramPacket(queryBytes, queryBytes.size, group, PORT)
                    socket.soTimeout = SOCKET_TIMEOUT
                    socket.send(datagramPacketRequest)

                    while (!socket.isClosed) {
                        readNextResponse(socket, callback)
                    }
                }

                cancelJob.join()
                discoverJob.join()
            } finally {
                Timber.i("Closing port $PORT for UPnP after ${System.currentTimeMillis() - start}")
            }
        }
    }

    private fun readNextResponse(socket: DatagramSocket, callback: (Service) -> Unit) {
        try {
            val datagramPacket = DatagramPacket(ByteArray(1024), 1024)
            socket.receive(datagramPacket)
            val response = String(datagramPacket.data, 0, datagramPacket.length)
            val isOk = response.uppercase().startsWith("HTTP/1.1 200")
            val uuidMatcher = uuidPattern.matcher(response)
            if (isOk) {
                val uuid = if (uuidMatcher.find()) {
                    uuidMatcher.group(1)
                } else {
                    Timber.i("No uuid in:\n$response")
                    return
                }
                Timber.v("Discovered: $uuid")
                val device = Service(
                    upnpHostname = "$UPNP_ADDRESS_PREFIX$uuid".lowercase(),
                    address = datagramPacket.address,
                    upnpId = uuid
                )

                BaseInjector.get().localDnsResolver().addUpnpDeviceToCache(device)
                callback(device)
            }
        } catch (e: SocketTimeoutException) {
            // Expected
        } catch (e: CancellationException) {
            // Expected
        } catch (e: SocketException) {
            // Socket closed
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    data class Service(
        val upnpHostname: String,
        val address: InetAddress,
        val upnpId: String,
    )
}