package de.crysxd.octoapp.base.network

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ShareCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_FULL_WEBCAM_RESOLUTION
import de.crysxd.octoapp.base.data.models.exceptions.SuppressedIllegalStateException
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.logging.TimberLogger
import de.crysxd.octoapp.base.utils.ByteArrayOutputStream2
import de.crysxd.octoapp.octoprint.SubjectAlternativeNameCompatVerifier
import de.crysxd.octoapp.octoprint.ext.withHostnameVerifier
import de.crysxd.octoapp.octoprint.ext.withSslKeystore
import de.crysxd.octoapp.octoprint.extractAndRemoveBasicAuth
import de.crysxd.octoapp.octoprint.interceptors.GenerateExceptionInterceptor
import de.crysxd.octoapp.octoprint.logging.LoggingInterceptorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.system.measureNanoTime

class MjpegConnection2(
    private val streamUrl: HttpUrl,
    name: String,
    private val throwExceptions: Boolean = false
) {

    private val tag = "MjpegConnection2/$name/${instanceCounter++}"
    private val bufferSize = 16_384
    private val tempCache = ByteArray(bufferSize)
    private val cache = ByteCache()
    private val timeoutMs = Firebase.remoteConfig.getLong("webcam_timeout_ms")
    private var response: Response? = null

    companion object {
        private var instanceCounter = 0
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun load(): Flow<MjpegSnapshot> {
        var hasBeenConnected = false
        var lastImageTime = System.currentTimeMillis()

        var imageTimes = 0L
        var imageCounter = 0
        var readTimeNs = 0L
        var searchTimeNs = 0L
        var decodeTimeNs = 0L

        return flow {
            emit(MjpegSnapshot.Loading)

            Timber.tag(tag).i("Connecting")
            val localResponse = connect()
            response = localResponse
            hasBeenConnected = true
            Timber.tag(tag).i("Connected, getting boundary")
            val boundary = extractBoundary(localResponse)
            Timber.tag(tag).i("Boundary extracted, starting to load images ($boundary)")

            cache.reset()
            val inputStream = localResponse.body!!.byteStream().buffered(bufferSize * 4)

            if (BaseInjector.get().octoPreferences().recordWebcamForDebug) {
                recordWebcamForDebug(inputStream)
            }

            while (true) {
                val (frame, analytics) = readNextImage(cache, boundary, inputStream)
                emit(MjpegSnapshot.Frame(frame, analytics))
            }
        }.onCompletion {
            Timber.tag(tag).i("Stopped stream")
            response?.body?.closeQuietly()
        }.onStart {
            Timber.tag(tag).i("Started stream")
        }.retryWhen { cause, attempt ->
            Timber.tag(tag).e(cause)

            if (throwExceptions) {
                throw cause
            }

            // If we had been connected in the past, wait 1s and try to reconnect once
            when {
                attempt >= 2 -> {
                    Timber.tag(tag).i("Reconnection attempt failed, escalating error")
                    false
                }
                hasBeenConnected -> {
                    val backoff = 2000 * (attempt + 1)
                    Timber.tag(tag).i("Connection broke down, scheduling reconnect (attempt=$attempt, backoff=${backoff}ms)")
                    emit(MjpegSnapshot.Loading)
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
            if (it is MjpegSnapshot.Frame) {
                val time = System.currentTimeMillis() - lastImageTime

                imageCounter++
                imageTimes += time
                searchTimeNs += it.analytics.searchTimeNs
                readTimeNs += it.analytics.readTimeNs
                decodeTimeNs += it.analytics.decodeTimeNs

                lastImageTime = System.currentTimeMillis()

                if (imageCounter > 20) {
                    Timber.tag(tag).i(
                        "FPS: %.1f (readTime=%.02f searchTime=%.02f decodeTime=%.02f)",
                        1000 / (imageTimes / imageCounter.toFloat()),
                        readTimeNs / 1_000_000f,
                        searchTimeNs / 1_000_000f,
                        decodeTimeNs / 1_000_000f,
                    )
                    imageCounter = 0
                    imageTimes = 0
                    readTimeNs = 0
                    searchTimeNs = 0
                    decodeTimeNs = 0
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun readNextImage(cache: ByteCache, boundary: String, input: InputStream, dropCount: Int = 0): Pair<Bitmap, Analytics> {
        var boundaryStart: Int? = null
        var boundaryEnd: Int? = null
        var nextOffset = 0
        require(dropCount < 5) { SuppressedIllegalStateException("Too many dropped frames") }
        var readTime = 0L
        var searchTime = 0L

        do {
            val read: Int
            readTime += measureNanoTime { read = input.read(tempCache) }
            if (read < 0) throw IOException("Connection broken")
            cache.push(tempCache, read)
            val bounds: IndexResult
            searchTime += measureNanoTime { bounds = cache.indexOf(nextOffset, boundary) }
            boundaryStart = bounds.start
            boundaryEnd = bounds.end
            nextOffset = bounds.nextOffset
        } while (boundaryStart == null || boundaryEnd == null)

        val frame: Bitmap?
        val decodeTime = measureNanoTime { frame = cache.readImage(boundaryStart, boundaryEnd) }
        return frame?.let {
            it to Analytics(
                readTimeNs = readTime,
                searchTimeNs = searchTime,
                decodeTimeNs = decodeTime,
                dropCount = dropCount,
            )
        } ?: readNextImage(cache, boundary, input, dropCount + 1)
    }

    private fun connect(): Response {
        val (url, authHeader) = streamUrl.extractAndRemoveBasicAuth()
        val sslKeystoreHandler = BaseInjector.get().sslKeyStoreHandler()
        val logger = TimberLogger(Logger.getLogger("$tag/HTTP")).logger
        val client = OkHttpClient.Builder()
            .dns(BaseInjector.get().localDnsResolver())
            .addInterceptor(GenerateExceptionInterceptor(null, null))
            .addInterceptor(HttpLoggingInterceptor(LoggingInterceptorLogger(logger)).setLevel(HttpLoggingInterceptor.Level.HEADERS))
            .withHostnameVerifier(SubjectAlternativeNameCompatVerifier().takeIf { sslKeystoreHandler.isWeakVerificationForHost(url) })
            .withSslKeystore(sslKeystoreHandler.loadKeyStore())
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .get()
            .also { builder -> authHeader?.let { builder.addHeader("Authorization", it) } }
            .url(url)
            .build()

        return client.newCall(request).execute()
    }

    private fun extractBoundary(response: Response): String {
        // Try to extract a boundary from HTTP header first.
        // If the information is not presented, throw an exception and use default value instead.
        val contentType: String = response.header("Content-Type") ?: throw Exception("Unable to get content type")
        val types = contentType.split(";".toRegex()).toTypedArray()
        if (types.none { it.startsWith("multipart/") }) {
            throw NoImageResourceException(contentType)
        }

        var extractedBoundary: String? = null
        for (ct in types) {
            val trimmedCt = ct.trim { it <= ' ' }
            if (trimmedCt.startsWith("boundary=")) {
                extractedBoundary = trimmedCt.removePrefix("boundary=").removePrefix("--") // Content after 'boundary='
            }
        }
        return if (extractedBoundary == null) {
            throw Exception("Unable to find mjpeg boundary from $types")
        } else if (extractedBoundary.first() == '"' && extractedBoundary.last() == '"') {
            "--" + extractedBoundary.removePrefix("\"").removeSuffix("\"")
        } else {
            "--$extractedBoundary"
        }
    }

    private class ByteCache {
        private val maxSize = 1024 * 1024 * 10L
        private val array = ByteArrayOutputStream2()
        private var bitmaps = emptyList<Bitmap>()
        private var lastBitmapUsed = 0
        private val bitmapPoolSize = 3
        private var sampleSize = 1

        fun reset() {
            array.reset()
            sampleSize = 1
            bitmaps = emptyList()
        }

        fun push(bytes: ByteArray, length: Int) {
            require((array.size() + length) < maxSize) { "Byte cached overflow: ${maxSize.asStyleFileSize()}" }
            array.write(bytes, 0, length)
        }

        fun readImage(length: Int, boundaryLength: Int): Bitmap? {
            val bitmap = if (length > 10) {
                try {
                    val bitmap = getNextBitmap(length)
                    val ops = BitmapFactory.Options()
                    ops.inBitmap = bitmap
                    ops.inSampleSize = sampleSize
                    BitmapFactory.decodeByteArray(array.toByteArray(), 0, length, ops)
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

        private fun getNextBitmap(length: Int): Bitmap {
            if (bitmaps.isEmpty()) {
                // Decode image bounds
                Timber.i("Creating bitmap pool")
                val ops = BitmapFactory.Options()
                ops.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(array.toByteArray(), 0, length, ops)

                // Calc used size
                val maxSize = if (BillingManager.isFeatureEnabled(FEATURE_FULL_WEBCAM_RESOLUTION)) {
                    Int.MAX_VALUE
                } else {
                    FirebaseRemoteConfig.getInstance().getLong("free_webcam_max_resolution").toInt()
                }
                sampleSize = 1
                var width = ops.outWidth
                var height = ops.outHeight
                while (width > maxSize || height > maxSize) {
                    sampleSize += 1
                    width = ops.outWidth / sampleSize
                    height = ops.outHeight / sampleSize
                }

                if (width != ops.outWidth || height != ops.outHeight) {
                    Timber.i("Native resolution of ${ops.outWidth}x${ops.outHeight}px exceeds maximum edge length of $maxSize")
                    Timber.i("Using sampleSize=$sampleSize, resulting in ${width}x${height}px")
                } else {
                    Timber.i("Resolution is ${ops.outWidth}x${ops.outHeight}px")
                }

                bitmaps = (0 until bitmapPoolSize).map {
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                }
            }

            lastBitmapUsed = (lastBitmapUsed + 1) % bitmaps.size
            return bitmaps[lastBitmapUsed]
        }

        private fun dropUntil(until: Int) {
            val length = array.size() - until
            if (length > 0) {
                val buf = ByteArray(length)
                System.arraycopy(array.toByteArray(), until, buf, 0, length)
                array.reset()
                array.write(buf)
            } else {
                array.reset()
            }
        }

        fun indexOf(offset: Int, boundaryStart: String): IndexResult {
            // Search start
            var startIndex = 0
            var startFound = false
            val length = array.size()
            val array = array.toByteArray()
            for (i in offset until length) {
                if (array[i].toInt().toChar() == boundaryStart[startIndex]) {
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

            if (!startFound) return IndexResult(nextOffset = length - boundaryStart.length)

            var endIndex = -1
            // Search end
            for (i in startIndex until length) {
                val c1 = array.getOrNull(i + 0)?.toInt()?.toChar()
                val c2 = array.getOrNull(i + 1)?.toInt()?.toChar()
                val c3 = array.getOrNull(i + 2)?.toInt()?.toChar()
                val c4 = array.getOrNull(i + 3)?.toInt()?.toChar()
                if (c1 == '\n' && c2 == '\n') {
                    endIndex = i + 2
                    break
                }
                if (c1 == '\r' && c2 == '\n' && c3 == '\r' && c4 == '\n') {
                    endIndex = i + 4
                    break
                }
            }

            return if (endIndex < 0) {
                IndexResult(nextOffset = startIndex)
            } else {
                IndexResult(start = startIndex, end = endIndex, nextOffset = startIndex)
            }
        }
    }

    private fun recordWebcamForDebug(input: InputStream) {
        Timber.i("Recording Mjpeg....")

        // Create file and store image
        val fileName = "recording-${SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())}.mjpg"
        val (file, uri) = BaseInjector.get().publicFileFactory().createPublicFile(fileName)
        file.outputStream().use { output ->
            val buffer = ByteArray(8196)
            var total = 0
            var read: Int
            var lastMajor = -1
            val majorDivider = 1024 * 1024
            val totalEnd = 4 * 1024 * 1024
            val totalMajor = totalEnd / majorDivider
            val start = System.currentTimeMillis()
            do {
                read = input.read(buffer)
                output.write(buffer, 0, read)
                total += read
                val major = total / majorDivider
                if (major > lastMajor) {
                    lastMajor = major
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(BaseInjector.get().localizedContext(), "Recording webcam $major/$totalMajor", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } while (read > 0 && total < totalEnd)
            val end = System.currentTimeMillis()
            output.write("Took ${end - start}ms".toByteArray())
        }

        // Share
        val mimeType = "x-octet-stream/*"
        Timber.i("Mjpeg recording ready, sharing...")
        val intent = ShareCompat.IntentBuilder(BaseInjector.get().localizedContext())
            .setStream(uri)
            .setChooserTitle("Webcam recording")
            .setType(mimeType)
            .createChooserIntent()
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        BaseInjector.get().localizedContext().startActivity(intent)

        BaseInjector.get().octoPreferences().recordWebcamForDebug = false
    }

    private data class IndexResult(
        val start: Int? = null,
        val end: Int? = null,
        val nextOffset: Int,
    )

    data class Analytics(
        val readTimeNs: Long,
        val searchTimeNs: Long,
        val decodeTimeNs: Long,
        val dropCount: Int,
    )

    sealed class MjpegSnapshot {
        object Loading : MjpegSnapshot()
        data class Frame constructor(val frame: Bitmap, val analytics: Analytics) : MjpegSnapshot()
    }

    class NoImageResourceException(mimeType: String) : IllegalStateException("No image resource: $mimeType")
}