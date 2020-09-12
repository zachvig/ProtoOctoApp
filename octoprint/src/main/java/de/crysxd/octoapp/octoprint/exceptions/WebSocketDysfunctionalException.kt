package de.crysxd.octoapp.octoprint.exceptions

class WebSocketDysfunctionalException : OctoPrintException(
    userMessage = "The web socket does not receive any messages. If you access OctoPrint through a proxy, make sure it supports web socket connection and HTTP connection upgrades.\n\nThis app does not function without a working web socket connection."
)