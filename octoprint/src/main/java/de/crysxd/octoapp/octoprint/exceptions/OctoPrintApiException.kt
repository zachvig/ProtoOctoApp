package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintApiException(httpUrl: HttpUrl, val responseCode: Int, body: String) : OctoPrintException(
    message = ProxyException.mask("Received unexpected response code $responseCode from $httpUrl (body=$body)", httpUrl.toString()),
    userFacingMessage = "There was an error in the communication with OctoPrint because an unexpected response was received."
)