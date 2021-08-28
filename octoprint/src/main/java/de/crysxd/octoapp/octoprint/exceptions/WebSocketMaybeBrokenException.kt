package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

abstract class WebSocketMaybeBrokenException(webUrl: HttpUrl, userFacingMessage: String) :
    OctoPrintException(userFacingMessage = userFacingMessage, webUrl = webUrl)