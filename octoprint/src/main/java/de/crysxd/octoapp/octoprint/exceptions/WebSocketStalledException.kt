package de.crysxd.octoapp.octoprint.exceptions

class WebSocketStalledException : OctoPrintException(userMessage = "The connection to OctoPrint is weak, information might not update in time.")