package de.crysxd.octoapp.octoprint.plugins.power

abstract class PowerDevice {
    abstract val displayName: String
    abstract val pluginDisplayName: String

    abstract suspend fun turnOn()
    abstract suspend fun turnOff()
    abstract suspend fun isOn(): Boolean

}