package de.crysxd.octoapp.octoprint.exceptions

class OctoEverywhereConnectionNotFoundException : OctoPrintException(
    userFacingMessage = "The connection with OctoEverywhere was revoked, please connect OctoEverywhere again.",
    technicalMessage = "OctoEverywhere reported conenction as deleted",
    webUrl = null,
    apiKey = null
)