package de.crysxd.octoapp.octoprint.plugins.companion

data class AppRegistrationBody(
    val fcmToken: String,
    val instanceId: String,
    val displayName: String,
    val model: String,
    val appVersion: String,
    val appBuild: Long,
    val appLanguage: String,
) {
    val command = "registerForNotifications"
}