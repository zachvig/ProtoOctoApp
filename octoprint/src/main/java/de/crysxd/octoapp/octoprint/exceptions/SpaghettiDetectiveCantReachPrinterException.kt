package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class SpaghettiDetectiveCantReachPrinterException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "Spaghetti Detective can't reach your OctoPrint at the moment",
    technicalMessage = "Received error code 482 from OctoEverywhere",
    webUrl = webUrl,
)