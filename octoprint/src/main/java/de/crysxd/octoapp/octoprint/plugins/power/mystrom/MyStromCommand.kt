package de.crysxd.octoapp.octoprint.plugins.power.mystrom

import de.crysxd.octoapp.octoprint.plugins.power.gpiocontrol.GpioControlPowerDevice
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuCommand

sealed class MyStromCommand(val command: String) {
    object EnableRelay : MyStromCommand("enableRelais")
    object DisableRelay : MyStromCommand("disableRelais")
}

