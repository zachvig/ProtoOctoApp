package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoEverywhereSubscriptionMissingException(webUrl: HttpUrl) : OctoPrintException(
    userFacingMessage = "<b>OctoEverywhere disabled</b><br><br>OctoEverywhere can't be used anymore as your supporter status expired. OctoEverywhere is disconnected for now, you can reconnect it at any time.",
    technicalMessage = "Missing supporter status",
    webUrl = webUrl,
    learnMoreLink = "https://octoeverywhere.com/appportal/v1/nosupporterperks?appid=octoapp"
), RemoteServiceConnectionBrokenException {
    override val remoteServiceName = "OctoEverywhere"
}