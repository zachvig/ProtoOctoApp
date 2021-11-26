package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoEverywhereConnectionNotFoundException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "The connection with OctoEverywhere was revoked, please connect OctoEverywhere again.",
    technicalMessage = "OctoEverywhere reported conenction as deleted",
    webUrl = webUrl,
), RemoteServiceConnectionBrokenException {
    override val remoteServiceName = "OctoEverywhere"
}