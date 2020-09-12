package de.crysxd.octoapp.octoprint.exceptions

class OctoPrintHttpsException(cause: Throwable) : OctoPrintException(
    cause = cause,
    userFacingMessage = "HTTPS connection could not be established. Make sure you installed all required certificates on your phone."
)