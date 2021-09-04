package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class AlternativeWebUrlException(message: String, webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = message,
    webUrl = webUrl
)