package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException

open class OctoPrintException(
    cause: Throwable? = null,
    open val userFacingMessage: String? = null,
    message: String? = userFacingMessage
) : IOException(message, cause)