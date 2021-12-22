package de.crysxd.octoapp.framework

import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.coroutines.runBlocking

object PsuUtils {
    fun OctoPrintInstanceInformationV3.turnAllOff() = runBlocking {
        val octoPrint = BaseInjector.get().octoPrintProvider().createAdHocOctoPrint(this@turnAllOff)
        val settings = octoPrint.createSettingsApi().getSettings()
        octoPrint.createPowerPluginsCollection().plugins.map { it.getDevices(settings) }.flatten().forEach { it.turnOff() }
    }
}