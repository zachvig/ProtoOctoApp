package de.crysxd.octoapp.octoprint.plugins.pluginmanager

data class PluginList(
    val plugins: List<Plugin>?
) {
    data class Plugin(
        val key: String,
        val enabled: Boolean?,
    )
}