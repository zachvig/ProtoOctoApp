package de.crysxd.octoapp.octoprint.exceptions

class OctoEverywhereSubscriptionMissingException : OctoPrintException(
    userFacingMessage = "OctoEverywhere can't be used anymore as your supporter status expired. OctoEverywhere is disconnected for now, you can reconnect OctoEverywhere at any time.",
    technicalMessage = "Missing supporter status",
    webUrl = null,
    apiKey = null,
)