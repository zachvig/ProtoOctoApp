package de.crysxd.octoapp.octoprint.exceptions

class OctoPrintUnavailableException(e: Exception? = null) : OctoPrintException(
    cause = e,
    userMessage = e?.localizedMessage ?: e?.message ?: "Unable to connect to OctoPrint"
)