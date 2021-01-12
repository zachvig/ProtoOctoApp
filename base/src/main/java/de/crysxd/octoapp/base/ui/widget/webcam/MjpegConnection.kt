package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
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
            val (boundaryPattern, input) = try {
                val connection = connect()
                val boundaryPattern = createHeaderBoundaryPattern(connection)
                val input = BufferedInputStream(connection.inputStream)
                Timber.i("Connected to $streamUrl")
                Pair(boundaryPattern, input)
            } catch (e: Exception) {
                emit(MjpegSnapshot.Error)
                delay(RECONNECT_TIMEOUT_MS)
                continue
            }

            // Read frames
            val buffer = ByteArray(4096)
            val lastBuffer = ByteArray(buffer.size * 2)
            var lastBufferLength = 0
            val imageBuffer = ByteArrayOutputStream()
            var lostFrameCount = 0
            while (true) {
                // Read data
                val bufferLength = input.read(buffer)
                if (bufferLength < 0) {
                    throw SocketException("Socket closed")
                }

                // Combine buffers and search for boundary
                System.arraycopy(buffer, 0, lastBuffer, lastBufferLength, bufferLength)
                val bufferStr = String(lastBuffer, 0, lastBufferLength + bufferLength, Charset.forName("ASCII"))

                // Check if frame is completed
                val matcher = boundaryPattern.matcher(bufferStr)
                if (matcher.find()) {
                    // Find boundary
                    val boundary = matcher.group(0)!!
                    val boundaryIndex = bufferStr.indexOf(boundary)

                    // Write missing data into imageBuffer
                    val missingLength = (boundaryIndex + boundary.length) - lastBufferLength
                    if (missingLength > 0) {
                        imageBuffer.write(lastBuffer, lastBufferLength, missingLength)
                    }

                    // Create bitmap
                    if (imageBuffer.size() - boundary.length > 0) {
                        val image = imageBuffer.toByteArray()
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
                    }

                    // Reset imageBuffer and write rest of data (next image)
                    try {


                        imageBuffer.reset()
                        val afterBoundaryIndex = boundaryIndex + boundary.length
                        val afterBoundaryLength = (lastBufferLength + bufferLength) - afterBoundaryIndex
                        imageBuffer.write(lastBuffer, afterBoundaryIndex, afterBoundaryLength)
                        System.arraycopy(lastBuffer, afterBoundaryIndex, lastBuffer, 0, afterBoundaryLength)
                        lastBufferLength = afterBoundaryLength
                    } catch (e: Exception) {
                        throw e
                    }
                } else {
                    // Write data
                    imageBuffer.write(buffer, 0, bufferLength)

                    // Store for next iteration
                    System.arraycopy(buffer, 0, lastBuffer, 0, bufferLength)
                    lastBufferLength = bufferLength
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
    }

    private fun connect() = (URL(streamUrl).openConnection() as HttpURLConnection).also {
        it.doInput = true
        it.connect()
    }

    private fun createHeaderBoundaryPattern(connection: HttpURLConnection): Pattern {
        val headerBoundary = extractBoundary(connection) ?: DEFAULT_HEADER_BOUNDARY

        // Determine boundary pattern
        // Use the whole header as separator in case boundary locate in difference chunks
        return Pattern.compile("(\\r\\n)?--$headerBoundary\\s+(.*)\\r\\n\\r\\n", Pattern.DOTALL)
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

    sealed class MjpegSnapshot {
        object Loading : MjpegSnapshot()
        object Error : MjpegSnapshot()
        data class Frame(val frame: Bitmap) : MjpegSnapshot()
    }
}