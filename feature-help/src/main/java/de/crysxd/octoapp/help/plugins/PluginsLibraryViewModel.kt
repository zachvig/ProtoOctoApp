package de.crysxd.octoapp.help.plugins

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.usecase.GetInstalledPluginsUseCase
import kotlinx.coroutines.launch
import timber.log.Timber

class PluginsLibraryViewModel(
    private val getInstalledPluginsUseCase: GetInstalledPluginsUseCase
) : BaseViewModel() {

    companion object {
        private val OCTOEVERYWHERE_ORDER = (100..1000).random()
        private val SPAGHETTI_DETECTIVE_ORDER = (100..1000).random()
        private val NGROK_ORDER = (0..100).random()
        private val TAILSCALE_ORDER = (0..100).random()
    }

    val pluginsIndex = MutableLiveData(
        PluginsIndex(
            listOf(
                PluginCategory(
                    name = "Recommended",
                    id = null,
                    plugins = listOf(
                        Plugin(
                            name = "OctoApp Companion",
                            key = "octoapp",
                            highlight = true,
                            order = 10_000,
                            description = "The Companion Plugin allows OctoApp to received notifications about your prints over the internet — more features coming soon!",
                            pluginPage = UriLibrary.getCompanionPluginUri(),
                        ),
                        Plugin(
                            name = "PSU Control",
                            key = "psucontrol",
                            highlight = true,
                            order = 9_999,
                            description = "Turn your printer on and off via the webinterface and OctoApp and let your printer turn off automatically after a print is done.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "OctoEverywhere",
                            highlight = true,
                            key = "octoeverywhere",
                            order = OCTOEVERYWHERE_ORDER,
                            description = "Easy remote access to OctoPrint so you can use OctoApp from wherever you are!",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=kvSLAsBHL00"),
                        ),
                        Plugin(
                            name = "The Spaghetti Detective",
                            highlight = true,
                            order = SPAGHETTI_DETECTIVE_ORDER,
                            key = "thespaghettidetective",
                            description = "Use the Spaghetti Detective's tunnel to use OctoApp when you are not at home!",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/thespaghettidetective/"),
//                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=kvSLAsBHL00"),
                        ),
                    ).sortedByDescending { it.order }
                ),
                PluginCategory(
                    name = "Remote access",
                    id = "remoteAccess",
                    plugins = listOf(
                        Plugin(
                            name = "ngrok",
                            key = "ngrok",
                            order = NGROK_ORDER,
                            description = "A plugin to securely access your OctoPrint instance remotely through ngrok",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=Rskcyzujhps"),
                        ),
                        Plugin(
                            name = "OctoEverywhere",
                            highlight = true,
                            key = "octoeverywhere",
                            order = OCTOEVERYWHERE_ORDER,
                            description = "Easy remote access to OctoPrint so you can use OctoApp from wherever you are!",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=kvSLAsBHL00"),
                        ),
                        Plugin(
                            name = "The Spaghetti Detective",
                            highlight = true,
                            key = "thespaghettidetective",
                            order = SPAGHETTI_DETECTIVE_ORDER,
                            description = "Use the Spaghetti Detective's tunnel to use OctoApp when you are not at home!",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/thespaghettidetective/"),
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=kOfhlZgye10"),
                        ),
                        Plugin(
                            name = "Tailscale",
                            key = "tailscale",
                            order = TAILSCALE_ORDER,
                            description = "Not a plugin — but a great way to easily enabled remote access",
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=2Ox1JJEEYoU"),
                        ),
                    ).sortedByDescending { it.order }
                ),
                PluginCategory(
                    name = "Power",
                    id = "power",
                    plugins = listOf(
                        Plugin(
                            name = "PSU Control",
                            key = "psucontrol",
                            highlight = true,
                            description = "Turn your printer on and off via the webinterface and OctoApp and let your printer turn off automatically after a print is done.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "GPIO Control",
                            key = "gpiocontrol",
                            description = "Control each device connected to your Raspberry Pi from the web interface and OctoApp.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/gpiocontrol/"),
                        ),
                        Plugin(
                            name = "Tasmota",
                            key = "tasmota",
                            description = "Simple plugin to control sonoff devices that have been flashed with Tasmota.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/tasmota/"),
                        ),
                        Plugin(
                            name = "TPLink Smart Plug",
                            key = "tplinksmartplug",
                            description = "Plugin to control TP-Link Smartplug devices from OctoPrint web interface and OctoApp",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/tplinksmartplug/"),
                        ),
                        Plugin(
                            name = "Tuya Smart Plug",
                            key = "psucontrol",
                            description = "Plugin to control Tuya based Smartplug devices from OctoPrint web interface and OctoApp",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "IKEA Tradfri",
                            key = "ikea_tradfri",
                            description = "Control Ikea Tradfri outlet from OctoPrint web interface and OctoApp",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/ikea_tradfri/"),
                        ),
                        Plugin(
                            name = "WS281x LED Status",
                            key = "ws281x_led_status",
                            description = "Add some WS281x type RGB LEDs to your printer for a quick status update! OctoApp can control the torch to illuminate your print.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/ws281x_led_status/"),
                        ),
                        Plugin(
                            name = "MyStrom",
                            key = "mystromswitch",
                            description = "Plugin to integrate myStrom Switch into your OctoPrint installation.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/mystromswitch/"),
                        ),
                        Plugin(
                            name = "OctoRelay",
                            key = "octorelay",
                            description = "A plugin to control relays or other things on the GPIO pins of your raspberry pi. For example turn the power of printer, the light or a fan ON and OFF via the web interface.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octorelay/"),
                        ),
                        Plugin(
                            name = "OctoLight",
                            key = "octolight",
                            description = "A simple plugin, that add's a button to the navbar, toggling GPIO on the RPi. It can be used for turning on and off a light.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octolight/"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Materials",
                    id = "materials",
                    plugins = listOf(
                        Plugin(
                            name = "FilamentManager",
                            key = "filamentmanager",
                            description = "This OctoPrint plugin helps to manage your filament spools.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/filamentmanager/"),
                        ),
                        Plugin(
                            name = "SpoolManager",
                            key = "SpoolManager",
                            description = "The OctoPrint-Plugin manages all spool informations and stores it in a database.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/SpoolManager/"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Files",
                    id = "files",
                    plugins = listOf(
                        Plugin(
                            name = "Cura Thumbnails",
                            key = "UltimakerFormatPackage",
                            description = "This plugin adds support for Ultimaker Format Package (.ufp) files.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/UltimakerFormatPackage/"),
                        ),
                        Plugin(
                            name = "Slicer Thumbnails",
                            key = "prusaslicerthumbnails",
                            description = "Extracts various slicer's embedded thumbnails from gcode files.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/prusaslicerthumbnails/"),
                        ),
                        Plugin(
                            name = "Upload Anything",
                            key = "uploadanything",
                            description = "Allows custom file types to be uploaded via the web interface and OctoApp.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/uploadanything/"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Others",
                    id = "others",
                    plugins = listOf(
                        Plugin(
                            name = "PrintTimeGenius",
                            key = "PrintTimeGenius",
                            highlight = true,
                            description = "Use a gcode pre-analysis to provide better print time estimation. OctoApp can show the improved estimations.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/PrintTimeGenius/"),
                        ),
                        Plugin(
                            name = "MultiCam",
                            key = "multicam",
                            description = "Extends the Control tab of OctoPrint and the webcam view in OctoApp, allowing the ability to switch between multiple webcam feeds.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/multicam/"),
                        ),
                        Plugin(
                            name = "ArcWelder",
                            key = "arc_welder",
                            description = "Anti-Stutter and GCode Compression. Replaces G0/G1 with G2/G3 where possible. OctoApp can show arcs generated in the Gcode preview.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/arc_welder/"),
                        ),
                    )
                )
            )
        )
    )

    init {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                val installed = getInstalledPluginsUseCase.execute(Unit)
                val current = pluginsIndex.value ?: return@launch
                val categories = current.categories.map { c ->
                    val plugins = c.plugins.map { p ->
                        p.copy(installed = installed.contains(p.key))
                    }
                    c.copy(plugins = plugins)
                }
                pluginsIndex.value = current.copy(categories = categories)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    data class PluginsIndex(
        val categories: List<PluginCategory>
    )

    data class PluginCategory(
        val plugins: List<Plugin>,
        val id: String?,
        val name: String,
    )

    data class Plugin(
        val key: String,
        val name: String,
        val description: String,
        val highlight: Boolean = false,
        val pluginPage: Uri? = null,
        val octoAppTutorial: Uri? = null,
        val installed: Boolean? = null,
        val order: Int = 0,
    )
}