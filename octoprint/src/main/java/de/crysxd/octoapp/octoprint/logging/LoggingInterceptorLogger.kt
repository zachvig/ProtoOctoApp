package de.crysxd.octoapp.octoprint.logging

import okhttp3.logging.HttpLoggingInterceptor
import java.util.logging.Level
import java.util.logging.Logger

class LoggingInterceptorLogger(
    private val logger: Logger
) : HttpLoggingInterceptor.Logger {

    override fun log(message: String) {
        if (message.startsWith("-->") || message.startsWith("<--")) {
            logger.log(Level.INFO, message)
        } else {
            logger.log(Level.FINEST, message)
        }
    }
}