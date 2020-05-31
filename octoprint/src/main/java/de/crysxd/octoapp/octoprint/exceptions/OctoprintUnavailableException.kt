package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException
import java.lang.Exception

class OctoprintUnavailableException(e: Exception? = null) : IOException(e)