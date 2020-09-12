package de.crysxd.octoapp.octoprint.exceptions

class OctoPrintUnavailableException(e: Exception? = null) : OctoPrintException(e, e?.localizedMessage ?: e?.message ?: "Unable to connect to OctoPrint")