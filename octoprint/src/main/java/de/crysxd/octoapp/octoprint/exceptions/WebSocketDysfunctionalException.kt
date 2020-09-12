package de.crysxd.octoapp.octoprint.exceptions

class WebSocketDysfunctionalException : OctoPrintException(message = "The web socket does not receive any messages. This may be caused by a poor proxy configuration.")