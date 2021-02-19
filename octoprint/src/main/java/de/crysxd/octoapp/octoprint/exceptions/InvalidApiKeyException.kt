package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class InvalidApiKeyException(httpUrl: HttpUrl? = null) : OctoPrintException(
    message = ProxyException.mask("OctoPrint reported an invalid API key when accessing $httpUrl", httpUrl.toString()),
    userFacingMessage = "OctoPrint reported the API key as invalid"
)