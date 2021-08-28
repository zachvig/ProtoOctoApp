package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class InvalidApiKeyException(httpUrl: HttpUrl) : OctoPrintException(
    webUrl = httpUrl,
    technicalMessage = "OctoPrint reported an invalid API key when accessing $httpUrl",
    userFacingMessage = "OctoPrint reported the API key as invalid"
)