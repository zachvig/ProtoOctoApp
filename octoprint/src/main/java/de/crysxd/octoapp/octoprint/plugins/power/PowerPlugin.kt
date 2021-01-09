package de.crysxd.octoapp.octoprint.plugins.power

import de.crysxd.octoapp.octoprint.models.settings.Settings

interface PowerPlugin<D : PowerDevice> {

    fun getDevices(settings: Settings): List<D>
}