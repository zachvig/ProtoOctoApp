package de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol

import com.google.gson.annotations.SerializedName
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import java.lang.IllegalStateException

data class GpioControlPowerDevice(
    private val plugin: GpioControlPowerPlugin? = null,
    @SerializedName("name") override val displayName: String,
    val index: Int
) : PowerDevice() {
    override val id = index.toString()
    override val pluginId = "gpiocontrol"
    override val pluginDisplayName = "GPIO Control"
    override val capabilities
        get() = listOf(Capability.ControlPrinterPower, Capability.Illuminate)

    override suspend fun turnOn() = plugin?.turnOn(this) ?: throw IllegalStateException("Acquire this class from GpioControlPowerPlugin")
    override suspend fun turnOff() = plugin?.turnOff(this) ?: throw IllegalStateException("Acquire this class from GpioControlPowerPlugin")
    override suspend fun isOn(): Boolean = plugin?.isOn(this) ?: throw IllegalStateException("Acquire this class from GpioControlPowerPlugin")
}
