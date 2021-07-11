package de.crysxd.octoapp.octoprint.plugins.power

abstract class PowerDevice {
    abstract val id: String
    abstract val pluginId: String
    abstract val displayName: String
    abstract val pluginDisplayName: String
    abstract val capabilities: List<Capability>
    open val controlMethods: List<ControlMethod> get() = listOf(ControlMethod.TurnOnOff, ControlMethod.Toggle)

    open suspend fun toggle() {
        val status = isOn() ?: throw IllegalStateException("Can't determine current state")
        if (status) turnOff() else turnOn()
    }

    abstract suspend fun turnOn()
    abstract suspend fun turnOff()
    abstract suspend fun isOn(): Boolean?

    val uniqueId
        get() = "$pluginId:$id"

    sealed class Capability {
        object ControlPrinterPower : Capability()
        object Illuminate : Capability()
    }

    sealed class ControlMethod {
        object TurnOnOff : ControlMethod()
        object Toggle : ControlMethod()
    }
}