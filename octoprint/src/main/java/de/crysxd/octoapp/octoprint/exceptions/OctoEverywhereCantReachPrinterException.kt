package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoEverywhereCantReachPrinterException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "OctoEverywhere can't reach your OctoPrint at the moment",
    technicalMessage = "Received error code 601 from OctoEverywhere",
    webUrl = webUrl,
)