package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class SpaghettiDetectiveTunnelUsageLimitReachedException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "You used up the data volume of your Spaghetti Detective tunnel.\n\nSpaghetti Detective is disconnected for now, you can reconnect again it in the „Configure Remote Access“ menu.",
    technicalMessage = "Received error code 481 from Spaghetti Detective",
    webUrl = webUrl,
), RemoteServiceConnectionBrokenException {
    override val remoteServiceName = "Spaghetti Detective"
}