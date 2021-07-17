package de.crysxd.octoapp.base.network

import android.content.Context
import android.net.wifi.WifiManager
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class OctoPrintUpnpDiscovery(
    context: Context,
    private val port: Int = 1900
) {
    companion object {
        const val UPNP_ADDRESS_PREFIX = "octoprint-via-upnp---"
        private const val SOCKET_TIMEOUT = 500
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

    suspend fun discover(timeout: Long, timeUnit: TimeUnit): List<Device> {
        val results = mutableListOf<Device>()

        withTimeoutOrNull(timeUnit.toMillis(timeout)) {
            discover { results.add(it) }
        }

        val unique = results.distinctBy { it.upnpId }
        Timber.i("Found ${unique.size} devices in $timeout $timeUnit")
        return unique
    }

    suspend fun discover(callback: (Device) -> Unit) {
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

    private suspend fun discoverWithMulticastLock(callback: (Device) -> Unit) = withContext(Dispatchers.IO) {
        Timber.i("Opening port $port")
        val socket = DatagramSocket(port)
        try {
            socket.reuseAddress = true
            val group = InetAddress.getByName(ADDRESS)
            val queryBytes = QUERY.toByteArray()
            val datagramPacketRequest = DatagramPacket(queryBytes, queryBytes.size, group, port)
            socket.soTimeout = SOCKET_TIMEOUT
            socket.send(datagramPacketRequest)

            while (currentCoroutineContext().isActive) {
                readNextResponse(socket, callback)
            }
        } finally {
            Timber.i("Closing port $port")
            socket.close()
        }
    }

    private fun readNextResponse(socket: DatagramSocket, callback: (Device) -> Unit) {
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
                    Timber.e(IllegalStateException("No uuid in $response"))
                    return
                }
                Timber.v("Discovered: $uuid")
                val device = Device(
                    upnpHostname = "$UPNP_ADDRESS_PREFIX$uuid",
                    address = datagramPacket.address,
                    upnpId = uuid
                )

                Injector.get().localDnsResolver().addUpnpDeviceToCache(device)
                callback(device)
            }
        } catch (e: SocketTimeoutException) {
            // Expected
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    data class Device(
        val upnpHostname: String,
        val address: InetAddress,
        val upnpId: String,
    )
}