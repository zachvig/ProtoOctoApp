package de.crysxd.octoapp.octoprint.plugins.power.ocotrelay

sealed class OctoRelayCommand(val command: String, open val pin: String) {
    data class Toggle(override val pin: String) : OctoRelayCommand("update", pin)
    data class GetStatus(override val pin: String) : OctoRelayCommand("getStatus", pin)
}
