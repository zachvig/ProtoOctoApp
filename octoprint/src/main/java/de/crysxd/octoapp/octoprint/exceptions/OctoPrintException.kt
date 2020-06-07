package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException

open class OctoPrintException(cause: Throwable? = null, message: String? = null) : IOException(message, cause)