package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintUnavailableException(e: Throwable? = null, webUrl: HttpUrl) : OctoPrintException(
    cause = e,
    webUrl = webUrl.toString(),
    technicalMessage = "${e?.message} $webUrl",
    userFacingMessage = e?.localizedMessage ?: e?.message ?: "Unable to connect to OctoPrint"
)