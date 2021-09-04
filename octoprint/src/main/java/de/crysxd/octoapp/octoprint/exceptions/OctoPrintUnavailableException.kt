package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintUnavailableException(e: Throwable? = null, webUrl: HttpUrl) : OctoPrintException(
    originalCause = e,
    webUrl = webUrl,
    userFacingMessage = "OctoPrint is not available, check your network connection.",
    technicalMessage = "${e?.message} $webUrl",
)