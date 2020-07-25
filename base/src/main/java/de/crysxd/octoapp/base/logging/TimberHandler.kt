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
                if (record.thrown == null) {
                    timber.v(record.message)
                } else {
                    timber.v(record.thrown, record.message)
                }
            Level.FINER, Level.FINE ->
                if (record.thrown == null) {
                    timber.d(record.message)
                } else {
                    timber.d(record.thrown, record.message)
                }
            Level.INFO ->
                if (record.thrown == null) {
                    timber.i(record.message)
                } else {
                    timber.i(record.thrown, record.message)
                }
            Level.WARNING ->
                if (record.thrown == null) {
                    timber.w(record.message)
                } else {
                    timber.w(record.thrown, record.message)
                }
            Level.SEVERE ->
                if (record.thrown == null) {
                    timber.e(record.message)
                } else {
                    timber.e(record.thrown, record.message)
                }
            else -> {
                Timber.wtf("Received log with unsupported level ${record.level.name}")
                if (record.thrown == null) {
                    timber.wtf(record.message)
                } else {
                    timber.wtf(record.thrown, record.message)
                }
            }
        }
    }

    override fun flush() = Unit

    override fun close() = Unit
}