package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class PrinterNotOperationalException(httpUrl: HttpUrl) : OctoPrintException(
    webUrl = httpUrl,
    technicalMessage = "Printer was not operational when accessing $httpUrl",
    userFacingMessage = "The printer is not ready to execute this task"
)