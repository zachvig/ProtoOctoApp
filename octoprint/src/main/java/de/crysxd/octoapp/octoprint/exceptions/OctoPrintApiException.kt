package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintApiException(httpUrl: HttpUrl, responseCode: Int, body: String) : OctoPrintException(
    message = "Received unexpected response code $responseCode from $httpUrl (body=$body)",
    userMessage = "There was an error in the communication with OctoPrint because an unexpected response was received."
)