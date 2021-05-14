package de.crysxd.octoapp.octoprint.plugins.power

abstract class PowerDevice {
    abstract val id: String
    abstract val pluginId: String
    abstract val displayName: String
    abstract val pluginDisplayName: String
    open val canControlPsu: Boolean = true

    abstract suspend fun turnOn()
    abstract suspend fun turnOff()
    abstract suspend fun isOn(): Boolean?

    val uniqueId
        get() = "$pluginId:$id"

}