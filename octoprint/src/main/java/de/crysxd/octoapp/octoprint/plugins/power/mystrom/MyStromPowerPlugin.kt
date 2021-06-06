package de.crysxd.octoapp.octoprint.plugins.power.mystrom

import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuCommand
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlApi
import de.crysxd.octoapp.octoprint.plugins.power.psucontrol.PsuControlPowerDevice

class MyStromPowerPlugin(
    private val myStromApi: MyStromApi
) : PowerPlugin<MyStromPowerDevice> {

    internal suspend fun turnOn() {
        myStromApi.sendCommand(MyStromCommand.EnableRelay)
    }

    internal suspend fun turnOff() {
        myStromApi.sendCommand(MyStromCommand.DisableRelay)
    }

    override fun getDevices(settings: Settings) = settings.plugins.values.mapNotNull {
        it as? Settings.MyStromSettings
    }.map {
        MyStromPowerDevice(this)
    }
}