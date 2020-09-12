package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class InvalidApiKeyException(httpUrl: HttpUrl) : OctoPrintException(
    message = "OctoPrint reported an invalid API key when accessing $httpUrl",
    userMessage = "OctoPrint reported the API key as invalid"
)