package de.crysxd.octoapp.framework

import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2

object TestEnvironmentLibrary {

    // Has SpoolManager
    val Frenchie = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5005",
        apiKey = "1AD1D1D17AF64B96A904771BBAF0AC2F"
    )

    // Vanilla
    val Terrier = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5004",
        apiKey = "0C56477AD8C342AC9D186960B18E0008"
    )


    val Beagle = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5001",
        apiKey = "88E504D6390B4476848DBAB9F243A7DA"
    )

    val Dachshund = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5003",
        apiKey = "24407743C66D4C1A9F4000467848CCF5"
    )

    val Corgi = OctoPrintInstanceInformationV2(
        webUrl = "http://${BuildConfig.TEST_ENV_DOMAIN}:5002",
        apiKey = "B9FC1B0110C94D99848CBCC5B66F67CF"
    )
}