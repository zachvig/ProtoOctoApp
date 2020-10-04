package de.crysxd.octoapp.octoprint.exceptions

class WebSocketStalledException : WebSocketMaybeBrokenException(userFacingMessage = "The connection to OctoPrint is weak, information might not update in time.")