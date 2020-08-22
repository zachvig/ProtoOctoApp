package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

const val RECONNECT_TIMEOUT_MS = 1000L
const val TOLERATED_FRAME_LOSS_STREAK = 4
const val DEFAULT_HEADER_BOUNDARY = "[_a-zA-Z0-9]*boundary"

class MjpegConnection(private val streamUrl: String) {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    fun load() = flow {
        while (true) {
            emit(MjpegSnapshot.Loading)

            // Connect
            val connection = connect()
            val boundaryPattern = createHeaderBoundaryPattern(connection)
            val input = BufferedInputStream(connection.inputStream)
            Timber.i("Connected to $streamUrl")

            // Read frames
            val buffer = ByteArray(4096)
            var image = ByteArray(0)
            var lostFrameCount = 0
            while (true) {
                // Read data
                val bufferLength = input.read(buffer)
                if (bufferLength < 0) {
                    throw SocketException("Socket closed")
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
                    } else {
                        lostFrameCount++
                        Timber.e("Lost frame due to decoding error (lostFrames=$lostFrameCount)")

                        if (lostFrameCount > TOLERATED_FRAME_LOSS_STREAK) {
                            throw IOException("Too many lost frames ($lostFrameCount)")
                        }
                    }

                    val headerIndex: Int = boundaryIndex + boundary.length
                    image = addByte(ByteArray(0), buffer, headerIndex, bufferLength - headerIndex)
                } else {
                    image = addByte(image, buffer, 0, bufferLength)
                }
            }
        }
    }.onCompletion {
        Timber.i("Stopped stream")
    }.onStart {
        Timber.i("Starting stream")
    }.retry {
        Timber.e(it)
        delay(RECONNECT_TIMEOUT_MS)
        true
    }.flowOn(Dispatchers.IO)

    private fun connect() = (URL(streamUrl).openConnection() as HttpURLConnection).also {
        it.doInput = true
        it.connect()
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
        }
        extractedBoundary
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
        object Error : MjpegSnapshot()
        data class Frame(val frame: Bitmap) : MjpegSnapshot()
    }
}