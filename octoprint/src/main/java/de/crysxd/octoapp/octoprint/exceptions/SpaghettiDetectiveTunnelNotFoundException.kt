package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class SpaghettiDetectiveTunnelNotFoundException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "The connection with Spaghetti Detective was revoked, please connect Spaghetti Detective again.",
    technicalMessage = "Spaghetti Detective reported tunnel as deleted",
    webUrl = webUrl,
), RemoteServiceConnectionBrokenException {
    override val remoteServiceName = "Spaghetti Detective"
}