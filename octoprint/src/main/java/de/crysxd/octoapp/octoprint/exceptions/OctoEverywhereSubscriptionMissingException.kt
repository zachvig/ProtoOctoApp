package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoEverywhereSubscriptionMissingException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "OctoEverywhere can't be used anymore as your supporter status expired. OctoEverywhere is disconnected for now, you can reconnect OctoEverywhere at any time.",
    technicalMessage = "Missing supporter status",
    webUrl = webUrl,
), RemoteServiceConnectionBrokenException {
    override val remoteServiceName = "OctoEverywhere"
}