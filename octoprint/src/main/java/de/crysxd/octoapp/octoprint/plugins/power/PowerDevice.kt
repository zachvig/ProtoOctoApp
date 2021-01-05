package de.crysxd.octoapp.octoprint.plugins.power

abstract class PowerDevice {
    abstract val displayName: CharSequence
    abstract val pluginDisplayName: CharSequence

    abstract suspend fun turnOn()
    abstract suspend fun turnOff()
    abstract suspend fun isOn(): Boolean

}