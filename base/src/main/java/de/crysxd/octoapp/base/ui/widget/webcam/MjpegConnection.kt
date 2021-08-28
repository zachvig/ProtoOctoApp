package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.crysxd.octoapp.octoprint.exceptions.ProxyException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

const val TOLERATED_FRAME_LOSS_STREAK = 4
const val DEFAULT_HEADER_BOUNDARY = "[_a-zA-Z0-9]*boundary"

@Deprecated("Use MjpegConnection2")
class MjpegConnection(private val streamUrl: String, private val authHeader: String?, private val name: String) {

    private val instanceId = instanceCounter++

    companion object {
        private var instanceCounter = 0
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun load(): Flow<MjpegSnapshot> {
        var hasBeenConnected = false
        return flow {
            try {
                while (true) {
                    emit(MjpegSnapshot.Loading)

                    // Connect
                    val connection = connect()
                    val boundaryPattern = createHeaderBoundaryPattern(connection)
                    val input = BufferedInputStream(connection.inputStream)
                    Timber.i("[$instanceId/$name] Connected to $streamUrl")

                    // Read frames
                    val buffer = ByteArray(4096)
                    var image = ByteArray(0)
                    var lostFrameCount = 0
                    while (true) {
                        // Read data
                        val bufferLength = input.read(buffer)
                        if (bufferLength < 0) {
                            throw SocketException("[$instanceId/$name] Socket closed")
                        }

                        // Append to image
                        val tmpCheckBoundry = addByte(image, buffer, 0, bufferLength)
                        val checkHeaderStr = String(tmpCheckBoundry, Charset.forName("ASCII"))

                        // Check if frame is completed
                        val matcher = boundaryPattern.matcher(checkHeaderStr)
                        if (matcher.find()) {
                            // Finalize image buffer
                            val boundary = matcher.group(0)!!
                            var boundaryIndex = checkHeaderStr.indexOf(boundary)
                            boundaryIndex -= image.size
                            image = if (boundaryIndex > 0) {
                                addByte(image, buffer, 0, boundaryIndex)
                            } else {
                                delByte(image, -boundaryIndex)
                            }

                            // Read bitmap
                            val outputImg = BitmapFactory.decodeByteArray(image, 0, image.size)
                            if (outputImg != null) {
                                lostFrameCount = 0
                                emit(MjpegSnapshot.Frame(outputImg))
                                hasBeenConnected = true
                            } else {
                                lostFrameCount++
                                Timber.e("[$instanceId/$name] Lost frame due to decoding error (lostFrames=$lostFrameCount)")

                                if (lostFrameCount > TOLERATED_FRAME_LOSS_STREAK) {
                                    throw IOException("[$instanceId/$name] Too many lost frames ($lostFrameCount)")
                                }
                            }

                            val headerIndex: Int = boundaryIndex + boundary.length
                            image = addByte(ByteArray(0), buffer, headerIndex, bufferLength - headerIndex)
                        } else {
                            image = addByte(image, buffer, 0, bufferLength)
                        }
                    }
                }
            } catch (e: Exception) {
                throw ProxyException.create(e, streamUrl.toHttpUrl())
            }
        }.onCompletion {
            Timber.i("[$instanceId/$name] Stopped stream")
        }.onStart {
            Timber.i("[$instanceId/$name] Starting stream")
        }.retryWhen { _, attempt ->
            // If we had been connected in the past, wait 1s and try to reconnect once
            when {
                attempt >= 2 -> {
                    Timber.i("Reconnection attempt failed, escalating error")
                    false
                }
                hasBeenConnected -> {
                    val backoff = 2000 * (attempt + 1)
                    Timber.i("Connection broke down, scheduling reconnect (attempt=$attempt, backoff=${backoff}ms)")
                    emit(MjpegSnapshot.Loading)
                    delay(backoff)
                    Timber.i("Reconnecting...")
                    true
                }
                else -> {
                    Timber.i("Connection broke down but never was connected, skipping reconnect")
                    false
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun connect(): HttpURLConnection {
        val url = URL(streamUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        // Basic Auth
        authHeader?.let {
            connection.setRequestProperty("Authorization", authHeader)
        }

        connection.doInput = true
        connection.connect()
        return connection
    }

    private fun createHeaderBoundaryPattern(connection: HttpURLConnection): Pattern {
        val headerBoundary = extractBoundary(connection) ?: DEFAULT_HEADER_BOUNDARY

        // Determine boundary pattern
        // Use the whole header as separator in case boundary locate in difference chunks
        return Pattern.compile("--$headerBoundary\\s+(.*)\\r\\n\\r\\n", Pattern.DOTALL)
    }

    private fun extractBoundary(connection: HttpURLConnection): String? = try {
        // Try to extract a boundary from HTTP header first.
        // If the information is not presented, throw an exception and use default value instead.
        val contentType: String = connection.getHeaderField("Content-Type") ?: throw java.lang.Exception("Unable to get content type")
        val types = contentType.split(";".toRegex()).toTypedArray()
        if (types.size == 0) {
            throw java.lang.Exception("Content type was empty")
        }
        var extractedBoundary: String? = null
        for (ct in types) {
            val trimmedCt = ct.trim { it <= ' ' }
            if (trimmedCt.startsWith("boundary=")) {
                extractedBoundary = trimmedCt.substring(9) // Content after 'boundary='
            }
        }
        if (extractedBoundary == null) {
            throw java.lang.Exception("Unable to find mjpeg boundary")
        } else if (extractedBoundary.first() == '"' && extractedBoundary.last() == '"') {
            extractedBoundary.removePrefix("\"").removeSuffix("\"")
        } else {
            extractedBoundary
        }
    } catch (e: java.lang.Exception) {
        Timber.w("Unable to extract header boundary")
        null
    }

    private fun addByte(base: ByteArray, add: ByteArray, addIndex: Int, length: Int): ByteArray {
        val tmp = ByteArray(base.size + length)
        System.arraycopy(base, 0, tmp, 0, base.size)
        System.arraycopy(add, addIndex, tmp, base.size, length)
        return tmp
    }

    private fun delByte(base: ByteArray, del: Int): ByteArray {
        val tmp = ByteArray(base.size - del)
        System.arraycopy(base, 0, tmp, 0, tmp.size)
        return tmp
    }

    sealed class MjpegSnapshot {
        object Loading : MjpegSnapshot()
        data class Frame(val frame: Bitmap) : MjpegSnapshot()
    }
}