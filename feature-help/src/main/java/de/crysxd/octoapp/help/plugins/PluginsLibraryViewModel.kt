package de.crysxd.octoapp.help.plugins

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import de.crysxd.baseui.BaseViewModel

class PluginsLibraryViewModel : BaseViewModel() {

    val mutablePluginsIndex = MutableLiveData(
        PluginsIndex(
            listOf(
                PluginCategory(
                    name = "Recommended",
                    plugins = emptyList()
                ),
                PluginCategory(
                    name = "Remote access",
                    plugins = listOf(
                        Plugin(
                            name = "OctoEverywhere",
                            description = "Free, simple, and secure remote monitoring and control of your OctoPrint printer anywhere in the world! Now including OctoPrint app support, printer notifications, and full framerate webcam streaming! Your full OctoPrint portal, plugins, apps, and webcam everywhere!",
                            installed = true,
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                        ),
                        Plugin(
                            name = "ngrok",
                            description = "A plugin to securely access your OctoPrint instance remotely through ngrok",
                            installed = false,
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                        ),
                        Plugin(
                            name = "Tailscale",
                            description = "Not a plugin — but a great way to easily enabled remote access",
                            installed = null,
                            octoAppTutorial = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            pluginPage = null,
                        ),
                    ).shuffled()
                ),
                PluginCategory(
                    name = "Power",
                    plugins = emptyList()
                ),
                PluginCategory(
                    name = "Materials",
                    plugins = emptyList()
                ),
                PluginCategory(
                    name = "Files",
                    plugins = emptyList()
                ),
                PluginCategory(
                    name = "Others",
                    plugins = emptyList()
                )
            )
        )
    )


    data class PluginsIndex(
        val categories: List<PluginCategory>
    )

    data class PluginCategory(
        val plugins: List<Plugin>,
        val name: String,
    )

    data class Plugin(
        val name: String,
        val description: String,
        val pluginPage: Uri?,
        val octoAppTutorial: Uri?,
        val installed: Boolean?,
    )

    data class InformationResource(
        val name: String,
        val uri: Uri,
    )
}