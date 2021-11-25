package de.crysxd.octoapp.octoprint.plugins.thespaghettidetective

sealed class SpaghettiDetectiveCommand(val command: String) {
    object GetPluginStatus : SpaghettiDetectiveCommand("get_plugin_status")
}