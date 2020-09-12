package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class PrinterNotOperationalException(httpUrl: HttpUrl) : OctoPrintException(
    message = "Printer was not operational when accessing $httpUrl",
    userMessage = "The printer is not ready to execute this task"
)