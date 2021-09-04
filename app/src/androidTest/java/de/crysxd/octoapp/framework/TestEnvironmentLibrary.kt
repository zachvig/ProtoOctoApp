package de.crysxd.octoapp.framework

import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2

object TestEnvironmentLibrary {

    // Has SpoolManager
    val Frenchie = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5005",
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )

    // Vanilla
    val Terrier = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5004",
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )


    val Beagle = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5001",
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )

    val Dachshund = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5003",
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )

    val Corgi = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5002",
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )
}