package de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol

import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuCommand

sealed class GpioControlCommand(val command: String, val id: String?) {
    class TurnGpioOn(device: GpioControlPowerDevice) : GpioControlCommand("turnGpioOn", device.id)
    class TurnGpioOff(device: GpioControlPowerDevice) : GpioControlCommand("turnGpioOff", device.id)
}

