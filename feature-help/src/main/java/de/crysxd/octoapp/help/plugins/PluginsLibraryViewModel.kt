package de.crysxd.octoapp.help.plugins

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.UriLibrary

class PluginsLibraryViewModel : BaseViewModel() {

    val pluginsIndex = MutableLiveData(
        PluginsIndex(
            listOf(
                PluginCategory(
                    name = "Recommended",
                    plugins = listOf(
                        Plugin(
                            name = "OctoApp Companion",
                            highlight = true,
                            description = "The Companion Plugin allows OctoApp to received notifications about your prints over the internet — more features coming soon!",
                            pluginPage = UriLibrary.getCompanionPluginUri(),
                        ),
                        Plugin(
                            name = "PSU Control",
                            highlight = true,
                            description = "Turn your printer on and off via the webinterface and OctoApp and let your printer turn off automatically after a print is done.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "OctoEverywhere",
                            highlight = true,
                            description = "Easy remote access to OctoPrint so you can use OctoApp from wherever you are!",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=kvSLAsBHL00"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Remote access",
                    plugins = listOf(
                        Plugin(
                            name = "OctoEverywhere",
                            highlight = true,
                            description = "Easy remote access to OctoPrint so you can use OctoApp from wherever you are!",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://www.youtube.com/watch?v=kvSLAsBHL00"),
                        ),
                        Plugin(
                            name = "ngrok",
                            description = "A plugin to securely access your OctoPrint instance remotely through ngrok",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                            octoAppTutorial = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                        ),
                        Plugin(
                            name = "Tailscale",
                            description = "Not a plugin — but a great way to easily enabled remote access",
                            octoAppTutorial = Uri.parse("https://plugins.octoprint.org/plugins/octoeverywhere/"),
                        ),
                    ).shuffled()
                ),
                PluginCategory(
                    name = "Power",
                    plugins = listOf(
                        Plugin(
                            name = "PSU Control",
                            highlight = true,
                            description = "Turn your printer on and off via the webinterface and OctoApp and let your printer turn off automatically after a print is done.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "GPIO Control",
                            description = "Control each device connected to your Raspberry Pi from the web interface and OctoApp.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/gpiocontrol/"),
                        ),
                        Plugin(
                            name = "Tasmota",
                            description = "Simple plugin to control sonoff devices that have been flashed with Tasmota.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/tasmota/"),
                        ),
                        Plugin(
                            name = "TPLink Smart Plug",
                            description = "Plugin to control TP-Link Smartplug devices from OctoPrint web interface and OctoApp",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/tplinksmartplug/"),
                        ),
                        Plugin(
                            name = "Tuya Smart Plug",
                            description = "Plugin to control Tuya based Smartplug devices from OctoPrint web interface and OctoApp",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "IKEA Tradfri",
                            description = "Control Ikea Tradfri outlet from OctoPrint web interface and OctoApp",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/ikea_tradfri/"),
                        ),
                        Plugin(
                            name = "WS281x LED Status",
                            description = "Add some WS281x type RGB LEDs to your printer for a quick status update! OctoApp can control the torch to illuminate your print.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/ws281x_led_status/"),
                        ),
                        Plugin(
                            name = "PSU Control",
                            description = "Turn your printer on and off via the webinterface and OctoApp and let your printer turn off automatically after a print is done.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/psucontrol/"),
                        ),
                        Plugin(
                            name = "MyStrom",
                            description = "Plugin to integrate myStrom Switch into your OctoPrint installation.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/mystromswitch/"),
                        ),
                        Plugin(
                            name = "OctoRelay",
                            description = "A plugin to control relays or other things on the GPIO pins of your raspberry pi. For example turn the power of printer, the light or a fan ON and OFF via the web interface.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octorelay/"),
                        ),
                        Plugin(
                            name = "OctoLight",
                            description = "A simple plugin, that add's a button to the navbar, toggleing GPIO on the RPi. It can be used for turning on and off a light.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/octolight/"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Materials",
                    plugins = listOf(
                        Plugin(
                            name = "FilamentManager",
                            description = "This OctoPrint plugin helps to manage your filament spools.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/filamentmanager/"),
                        ),
                        Plugin(
                            name = "SpoolManager",
                            description = "The OctoPrint-Plugin manages all spool informations and stores it in a database.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/SpoolManager/"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Files",
                    plugins = listOf(
                        Plugin(
                            name = "Cura Thumbnails",
                            description = "This plugin adds support for Ultimaker Format Package (.ufp) files.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/SpoolManager/"),
                        ),
                        Plugin(
                            name = "Slicer Thumbnails",
                            description = "Extracts various slicer's embedded thumbnails from gcode files.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/prusaslicerthumbnails/"),
                        ),
                        Plugin(
                            name = "Upload Anything",
                            description = "Allows custom file types to be uploaded via the web interface and OctoApp.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/uploadanything/"),
                        ),
                    )
                ),
                PluginCategory(
                    name = "Others",
                    plugins = listOf(
                        Plugin(
                            name = "PrintTimeGenius",
                            highlight = true,
                            description = "Use a gcode pre-analysis to provide better print time estimation. OctoApp can show the improved estimations.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/PrintTimeGenius/"),
                        ),
                        Plugin(
                            name = "MultiCam",
                            description = "Extends the Control tab of OctoPrint and the webcam view in OctoApp, allowing the ability to switch between multiple webcam feeds.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/multicam/"),
                        ),
                        Plugin(
                            name = "ArcWelder",
                            description = "Anti-Stutter and GCode Compression. Replaces G0/G1 with G2/G3 where possible. OctoApp can show arcs generated in the Gcode preview.",
                            pluginPage = Uri.parse("https://plugins.octoprint.org/plugins/arc_welder/"),
                        ),
                    )
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
        val highlight: Boolean = false,
        val pluginPage: Uri? = null,
        val octoAppTutorial: Uri? = null,
        val installed: Boolean? = null,
    )
}