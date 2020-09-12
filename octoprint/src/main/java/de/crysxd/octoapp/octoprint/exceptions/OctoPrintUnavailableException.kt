package de.crysxd.octoapp.octoprint.exceptions

class OctoPrintUnavailableException(e: Exception? = null) : OctoPrintException(
    cause = e,
    userFacingMessage = e?.localizedMessage ?: e?.message ?: "Unable to connect to OctoPrint"
)