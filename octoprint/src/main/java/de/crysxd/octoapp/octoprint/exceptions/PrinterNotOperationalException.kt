package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class PrinterNotOperationalException(httpUrl: HttpUrl) : OctoPrintException(
    message = ProxyException.mask("Printer was not operational when accessing $httpUrl", httpUrl.toString()),
    userFacingMessage = "The printer is not ready to execute this task"
)