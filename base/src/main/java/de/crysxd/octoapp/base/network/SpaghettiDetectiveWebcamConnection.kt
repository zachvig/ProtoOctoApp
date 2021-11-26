package de.crysxd.octoapp.base.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.crysxd.octoapp.octoprint.OctoPrint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.isActive
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class SpaghettiDetectiveWebcamConnection(
    private val octoPrint: OctoPrint,
    private val webcamIndex: Int,
    name: String,
) {
    companion object {
        private var instanceCounter = 0
    }

    private val tag = "SpaghettiCam/$name/${instanceCounter++}"
    private val Timber get() = timber.log.Timber.tag(tag)

    @Suppress("BlockingMethodInNonBlockingContext")
    fun load(): Flow<SpaghettiCamSnapshot> {
        return flow {
            emit(SpaghettiCamSnapshot.Loading)

            val client = octoPrint.createOkHttpClient()
            val spaghettiApi = octoPrint.createSpaghettiDetectiveApi()
            var bitmap: Bitmap? = null
            val byteCache = ByteArrayOutputStream()
            val options = BitmapFactory.Options()

            while (coroutineContext.isActive) {
                // Load frame URL
                Timber.d("Loading next frame")
                val frameUrl = spaghettiApi.getSpaghettiCamFrameUrl(webcamIndex)
                var delayMillis = 9_000L

                // Load frame
                frameUrl?.let {
                    try {
                        // Load image into memory
                        byteCache.reset()
                        val frame = client.newCall(Request.Builder().get().url(frameUrl).build()).execute()
                        frame.body?.byteStream()?.copyTo(byteCache)
                        delayMillis = frame.cacheControl.maxAgeSeconds.takeIf { it > 0 }?.let { TimeUnit.SECONDS.toMillis(it.toLong()) } ?: delayMillis

                        // Decode bounds and create bitmap
                        val bytes = byteCache.toByteArray()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        if (options.outWidth != bitmap?.width || options.outHeight != bitmap?.height) {
                            bitmap = Bitmap.createBitmap(options.outWidth, options.outHeight, Bitmap.Config.ARGB_8888)
                        }

                        // Create bitmap and emit
                        options.inJustDecodeBounds = false
                        options.inBitmap = bitmap
                        val out = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        emit(SpaghettiCamSnapshot.Frame(out))
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }

                // Publish and sleep
                Timber.d("Waiting $delayMillis + 1000ms for before getting next frame")
                delay(delayMillis + 1_000L)
            }
        }.retry(2) {
            Timber.e(it)
            true
        }
    }

    sealed class SpaghettiCamSnapshot {
        object Loading : SpaghettiCamSnapshot()
        data class Frame(val frame: Bitmap) : SpaghettiCamSnapshot()
    }
}