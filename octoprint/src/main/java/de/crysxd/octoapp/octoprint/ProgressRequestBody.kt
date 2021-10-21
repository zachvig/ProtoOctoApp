package de.crysxd.octoapp.octoprint

import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer


class ProgressRequestBody private constructor(private val wrapped: RequestBody, private val progressUpdate: (Float) -> Unit) : RequestBody() {

    override fun contentType() = wrapped.contentType()

    override fun writeTo(sink: BufferedSink) {
        val cs = CountingSink(sink, contentLength(), progressUpdate)
        val buffered = cs.buffer()
        wrapped.writeTo(buffered)
        buffered.flush()
    }

    override fun contentLength() = wrapped.contentLength()

    override fun isDuplex() = wrapped.isDuplex()

    override fun isOneShot() = wrapped.isOneShot()

    private class CountingSink(delegate: Sink, private val contentLength: Long, private val progressUpdate: (Float) -> Unit) : ForwardingSink(delegate) {
        private var bytesWritten = 0L
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            progressUpdate(byteCount / contentLength.toFloat())
        }
    }

    companion object {
        fun RequestBody.asProgressRequestBody(progressUpdate: (Float) -> Unit) = ProgressRequestBody(this, progressUpdate)
    }
}

