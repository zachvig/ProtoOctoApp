package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException
import java.lang.Exception

class OctoPrintUnavailableException(e: Exception? = null) : IOException(e)