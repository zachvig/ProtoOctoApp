package de.crysxd.octoapp.octoprint.exceptions

class AlternativeWebUrlException(message: String, webUrl: String) : OctoPrintException(
    userFacingMessage = message,
    webUrl = webUrl
)