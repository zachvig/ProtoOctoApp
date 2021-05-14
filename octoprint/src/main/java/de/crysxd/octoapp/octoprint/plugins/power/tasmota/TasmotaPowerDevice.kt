package de.crysxd.octoapp.octoprint.plugins.power.tasmota

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice

data class TasmotaPowerDevice(
    val ip: String,
    val idx: String,
    @Transient val plugin: TasmotaPowerPlugin?,
    @SerializedName("label") override val displayName: String,
) : PowerDevice() {
    override val capabilities
        get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    override val id
        get() = "$ip/$idx"

    override val pluginDisplayName
        get() = "Tasmota"

    override val pluginId
        get() = "tasmota"

    override suspend fun turnOn() = plugin?.turnOn(this)
        ?: throw IllegalStateException("Acquire this class from TasmotaPowerPlugin!")

    override suspend fun turnOff() = plugin?.turnOff(this)
        ?: throw IllegalStateException("Acquire this class from TasmotaPowerPlugin!")

    override suspend fun isOn() = plugin?.isOn(this)
        ?: throw IllegalStateException("Acquire this class from TasmotaPowerPlugin!")

}