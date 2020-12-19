package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class InvalidApiKeyException(httpUrl: HttpUrl? = null) : OctoPrintException(
    message = "OctoPrint reported an invalid API key when accessing $httpUrl",
    userFacingMessage = "OctoPrint reported the API key as invalid"
)