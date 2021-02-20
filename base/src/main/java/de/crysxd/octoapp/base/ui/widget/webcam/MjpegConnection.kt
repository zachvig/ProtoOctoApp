package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

const val RECONNECT_TIMEOUT_MS = 1000L
const val TOLERATED_FRAME_LOSS_STREAK = 4
const val DEFAULT_HEADER_BOUNDARY = "[_a-zA-Z0-9]*boundary"

class MjpegConnection(private val streamUrl: String) {

    companion object {
        var instanceCounter = 0
    }

    private val instanceId = instanceCounter++

    @Suppress("BlockingMethodInNonBlockingContext", "ExperimentalApiUsage")
    fun load() = flow {
        while (true) {
            emit(MjpegSnapshot.Loading)

            // Connect
            val connection = connect()
            val boundaryPattern = createHeaderBoundaryPattern(connection)
            val input = BufferedInputStream(connection.inputStream)
            Timber.i("[$instanceId] Connected to $streamUrl")

            // Read frames
            val buffer = ByteArray(4096)
            val lastBuffer = ByteArray(buffer.size * 2)
            var lastBufferLength = 0
            val imageBuffer = ByteArrayOutputStream()
            var lostFrameCount = 0
            var bitmapOptions: BitmapFactory.Options? = null
            while (true) {
                // Read data
                val bufferLength = input.read(buffer)
                if (bufferLength < 0) {
                    throw SocketException("[$instanceId] Socket closed")
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
                        // Create the bitmap options. This allows use to reuse the same Bitmap over and over
                        if (bitmapOptions == null) {
                            Timber.i("[$instanceId] Init options")
                            // Read bitmap bounds
                            bitmapOptions = BitmapFactory.Options()
                            bitmapOptions.inJustDecodeBounds = true
                            readBitmap(imageBuffer, bitmapOptions)

                            // Create the bitmap we will continue to reuse
                            val bitmap = Bitmap.createBitmap(bitmapOptions.outHeight, bitmapOptions.outWidth, Bitmap.Config.ARGB_8888)

                            // Configure options to reuse bitmap
                            bitmapOptions.inBitmap = bitmap
                            bitmapOptions.inJustDecodeBounds = false
                            bitmapOptions.inSampleSize = 1
                            Timber.i("[$instanceId] Options created (${bitmapOptions.outWidth}x${bitmapOptions.outHeight} px)")
                        }
                        readBitmap(imageBuffer, bitmapOptions)
                        val bitmap = bitmapOptions.inBitmap
                        if (bitmap != null) {
                            lostFrameCount = 0
                            emit(MjpegSnapshot.Frame(bitmapOptions.inBitmap))
                        } else {
                            lostFrameCount++
                            Timber.e("[$instanceId] Lost frame due to decoding error (lostFrames=$lostFrameCount)")

                            if (lostFrameCount > TOLERATED_FRAME_LOSS_STREAK) {
                                throw IOException("[$instanceId] Too many lost frames ($lostFrameCount)")
                            }
                        }
                    }

                    // Reset imageBuffer and write rest of data (next image)
                    imageBuffer.reset()
                    val afterBoundaryIndex = boundaryIndex + boundary.length
                    val afterBoundaryLength = (lastBufferLength + bufferLength) - afterBoundaryIndex
                    imageBuffer.write(lastBuffer, afterBoundaryIndex, afterBoundaryLength)
                    System.arraycopy(lastBuffer, afterBoundaryIndex, lastBuffer, 0, afterBoundaryLength)
                    lastBufferLength = afterBoundaryLength
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
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun readBitmap(image: ByteArrayOutputStream, options: BitmapFactory.Options) = withContext(Dispatchers.IO) {
        val inStream = PipedInputStream()
        val outStream = PipedOutputStream(inStream)
        val job = launch {
            try {
                image.writeTo(outStream)
                outStream.close()
            } catch (e: IOException) {
                // Usual exception when only reading bounds of the bitmap (caused be calling close after decodeStream())
            }
        }
        BitmapFactory.decodeStream(inStream, null, options)
        outStream.close()
        job.cancelAndJoin()
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
        val contentType: String = connection.getHeaderField("Content-Type") ?: throw Exception("Unable to get content type")
        val types = contentType.split(";".toRegex()).toTypedArray()
        if (types.isEmpty()) {
            throw Exception("Content type was empty")
        }
        var extractedBoundary: String? = null
        for (ct in types) {
            val trimmedCt = ct.trim { it <= ' ' }
            if (trimmedCt.startsWith("boundary=")) {
                extractedBoundary = trimmedCt.substring(9) // Content after 'boundary='
            }
        }
        if (extractedBoundary == null) {
            throw Exception("Unable to find mjpeg boundary")
        } else if (extractedBoundary.first() == '"' && extractedBoundary.last() == '"') {
            extractedBoundary.removePrefix("\"").removeSuffix("\"")
        } else {
            extractedBoundary
        }
    } catch (e: Exception) {
        Timber.w("Unable to extract header boundary")
        null
    }

    sealed class MjpegSnapshot {
        object Loading : MjpegSnapshot()
        data class Frame(val frame: Bitmap) : MjpegSnapshot()
    }
}