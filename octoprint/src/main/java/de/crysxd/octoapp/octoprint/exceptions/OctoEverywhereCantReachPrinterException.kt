package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException

class OctoEverywhereCantReachPrinterException : OctoPrintException(
    userFacingMessage = "OctoEverywhere can't reach your OctoPrint at the moment",
    technicalMessage = "Received error code 601 from OctoEverywhere",
    webUrl = null,
    apiKey = null
)