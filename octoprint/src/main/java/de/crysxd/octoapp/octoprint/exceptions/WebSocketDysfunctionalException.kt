package de.crysxd.octoapp.octoprint.exceptions

class WebSocketDysfunctionalException : WebSocketMaybeBrokenException(
    userFacingMessage = "The web socket does not receive any messages.\n\nThis app does not function without a working web socket connection!\n\nIf you access OctoPrint through a proxy, make sure it supports web socket connections and HTTP connection upgrades. A poorly configured proxy is the most common cause for this issue."
)