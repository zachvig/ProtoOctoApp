package de.crysxd.octoapp.webcam

import android.content.Context
import de.crysxd.octoapp.base.utils.AppScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.closeQuietly
import timber.log.Timber
import java.net.ServerSocket

class MjpegTestServer(val port: Int = 8000) {

    companion object {
        private val BOUNDARY = "BOUNDARYDONOTCROSS"
        private val NL = "\n"
        private val HEADERS = """
            HTTP/1.1 200 OK
            Connection: close
            Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0
            Expires: -1
            Access-Control-Allow-Origin: *
            Server: Test
            Pragma: no-cache
            Content-Type: multipart/x-mixed-replace;boundary=$BOUNDARY$NL$NL
        """.trimIndent()
        private val SEPARATOR = """
            $NL--$BOUNDARY
            Content-Type: image/jpeg
            Content-Length: %d$NL$NL
        """.trimIndent()
    }

    private var ss: ServerSocket? = null
    val frameCount = 120

    fun start(context: Context) = AppScope.launch(Dispatchers.IO) {
        require(ss == null)
        ss = ServerSocket(port)
        while (true) {
            try {
                val s = requireNotNull(ss).accept()
                val out = s.getOutputStream().buffered()
                val writer = out.bufferedWriter()
                writer.write(HEADERS)
                Timber.i("Sent headers")
                val imageFormat = "webcam_test_1080_%d"
                while (true) {
                    Timber.i("Sending frames...")
                    (1..frameCount).forEach { i ->
                        val id = context.resources.getIdentifier(String.format(imageFormat, i), "raw", context.packageName)
                        context.resources.openRawResource(id).use {
                            writer.write(String.format(SEPARATOR, it.available()))
                            writer.flush()
                            it.copyTo(out)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun stop() = ss?.closeQuietly()
}