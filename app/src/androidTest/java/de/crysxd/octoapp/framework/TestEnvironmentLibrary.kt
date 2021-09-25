package de.crysxd.octoapp.framework

import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import okhttp3.HttpUrl.Companion.toHttpUrl

object TestEnvironmentLibrary {

    // Has SpoolManager
    val Frenchie = OctoPrintInstanceInformationV3(
        id = "frenchie",
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5005".toHttpUrl(),
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )

    // Vanilla
    val Terrier = OctoPrintInstanceInformationV3(
        id = "terrier",
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5004".toHttpUrl(),
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )


    val Beagle = OctoPrintInstanceInformationV3(
        id = "beagle",
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5001".toHttpUrl(),
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )

    val Dachshund = OctoPrintInstanceInformationV3(
        id = "dachshund",
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5003".toHttpUrl(),
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )

    val Corgi = OctoPrintInstanceInformationV3(
        id = "corgi",
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5002".toHttpUrl(),
        apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
    )
}