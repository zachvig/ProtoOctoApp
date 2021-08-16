package de.crysxd.octoapp.octoprint.plugins.power.tradfri

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class TradfriPowerDevice(
    // The plugin gives back a Int or a String as id depending on its mood...we get it as any and clean it up below
    @SerializedName("id") val idAny: Any,
    @Transient val plugin: TradfriPowerPlugin?,
    @SerializedName("name") override val displayName: String,
) : PowerDevice() {
    override val capabilities
        get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    override val id: String
        get() = when (idAny) {
            is Number -> idAny.toInt().toString()
            is String -> idAny
            else -> throw java.lang.IllegalStateException("idAny is $idAny")
        }

    // We need to copy ourselves for serialization to make sure we have a string in the idAny field... 🙄
    fun asSerializableVersion() = copy(idAny = id)

    @Transient
    override val pluginDisplayName = "Trådfri"

    @Transient
    override val pluginId = "tradfri"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TradfriPowerPlugin!")

}