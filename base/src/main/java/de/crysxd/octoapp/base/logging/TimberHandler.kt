package de.crysxd.octoapp.base.logging

import timber.log.Timber
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

class TimberHandler : Handler() {

    override fun publish(record: LogRecord) {
        val timber = Timber.tag(record.loggerName)

        when (record.level) {
            Level.OFF ->
                Unit
            Level.FINEST ->
                timber.v(record.thrown, record.message)
            Level.FINER, Level.FINE ->
                timber.d(record.thrown, record.message)
            Level.INFO ->
                timber.i(record.thrown, record.message)
            Level.WARNING ->
                timber.w(record.thrown, record.message)
            Level.SEVERE ->
                timber.e(record.thrown, record.message)
            else -> {
                Timber.wtf("Received log with unsupported level ${record.level.name}")
                Timber.wtf(record.thrown, record.message)
            }
        }
    }

    override fun flush() = Unit

    override fun close() = Unit
}