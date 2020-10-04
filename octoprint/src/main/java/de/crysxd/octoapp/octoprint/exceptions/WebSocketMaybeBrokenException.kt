package de.crysxd.octoapp.octoprint.exceptions

abstract class WebSocketMaybeBrokenException(userFacingMessage: String, message: String? = null) :
    OctoPrintException(userFacingMessage = userFacingMessage, message = message)