package de.crysxd.octoapp.octoprint.exceptions

import de.crysxd.octoapp.octoprint.redactLoggingString
import okhttp3.HttpUrl
import java.io.IOException
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

open class OctoPrintException(
    open val userFacingMessage: String,
    val webUrl: HttpUrl,
    val originalCause: Throwable? = null,
    val technicalMessage: String = userFacingMessage,
) : IOException(webUrl.redactLoggingString(technicalMessage), originalCause?.let { ProxyException.create(it, webUrl) })