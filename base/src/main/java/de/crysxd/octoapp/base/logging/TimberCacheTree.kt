package de.crysxd.octoapp.base.logging

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class TimberCacheTree(
    private val mask: SensitiveDataMask,
    private val maxSize: Int = 1024 * 128
) : Timber.DebugTree() {

    private val maxMessageLength = 4000
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS", Locale.ENGLISH)
    private val cache = StringBuilder()
    private val lock = Mutex()

    val logs get() = cache.toString()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority > Log.VERBOSE) {
            GlobalScope.launch(Dispatchers.IO) {
                lock.withLock {
                    val prefix = "${getTime()} ${getLevel(priority)}/${tag ?: "???"}: "
                    cache.append(prefix)
                    cache.append(mask.mask(message.substring(0, message.length.coerceAtMost(maxMessageLength))))
                    cache.append("\n")

                    t?.let {
                        val stackTrace = StringWriter()
                        val writer = PrintWriter(stackTrace)
                        t.printStackTrace(writer)

                        stackTrace.buffer.lines().forEach {
                            cache.append(prefix)
                            cache.append(mask.mask(it))
                            cache.append("\n")
                        }
                    }

                    if (cache.length > maxSize) {
                        cache.delete(0, cache.length - maxSize)
                    }
                }
            }
        }
    }

    private fun getTime() = dateFormat.format(Date())

    private fun getLevel(priority: Int) = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }
}