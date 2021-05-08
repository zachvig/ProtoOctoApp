package de.crysxd.octoapp.base.ui.widget.webcam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class MjpegConnection2(private val streamUrl: String, private val authHeader: String?, private val name: String) {

    private val tag = "MjpegConnection2/$name/${instanceCounter++}"
    private val bufferSize = 16_384
    private val tempCache = ByteArray(bufferSize)
    private val cache = ByteCache()

    companion object {
        private var instanceCounter = 0
        private const val DEFAULT_HEADER_BOUNDARY = "[_a-zA-Z0-9]*boundary"

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun load(): Flow<MjpegConnection.MjpegSnapshot> {
        var hasBeenConnected = false
        var lastImageTime = System.currentTimeMillis()
        var imageTimes = 0L
        var imageCounter = 0
        return flow {
            emit(MjpegConnection.MjpegSnapshot.Loading)

            Timber.tag(tag).i("Connecting")
            val connection = connect()
            hasBeenConnected = true
            Timber.tag(tag).i("Connected, getting boundary")
            val boundary = extractBoundary(connection) ?: DEFAULT_HEADER_BOUNDARY
            Timber.tag(tag).i("Boundary extracted, starting to load images")

            cache.reset()
            val inputStream = connection.inputStream.buffered(bufferSize * 4)
            while (true) {
                emit(
                    MjpegConnection.MjpegSnapshot.Frame(
                        readNextImage(cache, boundary, inputStream)
                    )
                )
            }
        }.onCompletion {
            Timber.tag(tag).i("Stopped stream")
        }.onStart {
            Timber.tag(tag).i("Started stream")
        }.retryWhen { cause, attempt ->
            Timber.tag(tag).e(cause)

            // If we had been connected in the past, wait 1s and try to reconnect once
            when {
                attempt >= 2 -> {
                    Timber.tag(tag).i("Reconnection attempt failed, escalating error")
                    false
                }
                hasBeenConnected -> {
                    val backoff = 2000 * (attempt + 1)
                    Timber.tag(tag).i("Connection broke down, scheduling reconnect (attempt=$attempt, backoff=${backoff}ms)")
                    emit(MjpegConnection.MjpegSnapshot.Loading)
                    delay(backoff)
                    Timber.tag(tag).i("Reconnecting...")
                    true
                }
                else -> {
                    Timber.tag(tag).i("Connection broke down but never was connected, skipping reconnect")
                    false
                }
            }
        }.onEach {
            imageCounter++
            val time = System.currentTimeMillis() - lastImageTime
            imageTimes += time
            lastImageTime = System.currentTimeMillis()
            if (imageCounter > 100) {
                Timber.tag(tag).i("FPS: %.1f", 1000 / (imageTimes / imageCounter.toFloat()))
                imageCounter = 0
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun readNextImage(cache: ByteCache, boundary: String, input: InputStream, dropCount: Int = 0): Bitmap {
        var boundaryStart: Int?
        var boundaryEnd: Int?
        require(dropCount < 5) { "Too many dropped frames" }
        do {
            //Timber.i("Available: ${input.available().toLong().asStyleFileSize()}")
            val read = input.read(tempCache)
            if (read < 0) throw IOException("Connection broken")
            cache.push(tempCache, read)
            val bounds = cache.indexOf(boundary)
            boundaryStart = bounds.first
            boundaryEnd = bounds.second
        } while (boundaryStart == null || boundaryEnd == null)
        return cache.readImage(boundaryStart, boundaryEnd) ?: readNextImage(cache, boundary, input, dropCount + 1)
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

    private fun extractBoundary(connection: HttpURLConnection): String? = try {
        // Try to extract a boundary from HTTP header first.
        // If the information is not presented, throw an exception and use default value instead.
        val contentType: String = connection.getHeaderField("Content-Type") ?: throw java.lang.Exception("Unable to get content type")
        val types = contentType.split(";".toRegex()).toTypedArray()
        if (types.isEmpty()) {
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
            "--" + extractedBoundary.removePrefix("\"").removeSuffix("\"")
        } else {
            "--$extractedBoundary"
        }
    } catch (e: Exception) {
        Timber.w("Unable to extract header boundary")
        null
    }

    private class ByteCache(val size: Int = 1024 * 1024 * 2) {
        val array = ByteArray(size)
        var index: Int = 0

        fun reset() {
            index = 0
        }

        fun push(bytes: ByteArray, length: Int) {
            require(index + length < array.size) { "Byte cached overflow: ${index + length} exceeds $size" }
            bytes.copyInto(array, index, startIndex = 0, endIndex = length)
            index += length
        }

        fun readImage(length: Int, boundaryLength: Int): Bitmap? {
            val bitmap = if (length > 10) {
                try {
                    BitmapFactory.decodeByteArray(array, 0, length)
                } catch (e: Exception) {
                    Timber.w("Failed to decode frame: $e")
                    null
                }
            } else {
                null
            }
            dropUntil(boundaryLength)
            return bitmap
        }

        fun dropUntil(until: Int) {
            val length = (index - until).coerceAtLeast(0)
            System.arraycopy(array, until, array, 0, length)
            index = length
        }

        fun indexOf(boundaryStart: String): Pair<Int?, Int?> {
            // Search start
            var startIndex = 0
            var startFound = false
            for (i in 0 until index) {
                if (array[i].toChar() == boundaryStart[startIndex]) {
                    startIndex++
                } else {
                    startIndex = 0
                }

                if (startIndex == boundaryStart.length) {
                    startFound = true
                    startIndex = i - boundaryStart.length + 1
                    break
                }
            }

            if (!startFound) return null to null

            // Search end
            var endIndex = -1
            for (i in startIndex until index) {
                val c1 = array[i].toChar()
                val c2 = array[i + 1].toChar()
                val c3 = array[i + 2].toChar()
                val c4 = array[i + 3].toChar()
                if (c1 == '\n' && c2 == '\n') {
                    endIndex = i + 2
                    break
                }
                if (c1 == '\r' && c2 == '\n' && c3 == '\r' && c4 == '\n') {
                    endIndex = i + 4
                    break
                }
            }

            if (endIndex < 0) return null to null
            return startIndex to endIndex
        }
    }
}