package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintUnavailableException(e: Throwable? = null, webUrl: HttpUrl) : OctoPrintException(
    originalCause = e,
    webUrl = webUrl.toString(),
    technicalMessage = "${e?.message} $webUrl",
)