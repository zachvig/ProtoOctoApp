package de.crysxd.octoapp.octoprint.exceptions

import de.crysxd.octoapp.octoprint.redactLoggingString
import okhttp3.HttpUrl
import java.io.IOException

open class OctoPrintException(
    open val userFacingMessage: String,
    val webUrl: HttpUrl,
    val originalCause: Throwable? = null,
    val technicalMessage: String = userFacingMessage,
    val learnMoreLink: String? = null,
) : IOException(webUrl.redactLoggingString(technicalMessage), originalCause?.let { ProxyException.create(it, webUrl) })