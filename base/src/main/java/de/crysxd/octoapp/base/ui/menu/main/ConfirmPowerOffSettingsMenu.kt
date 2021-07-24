package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuHost
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.parcelize.Parcelize

@Parcelize
class ConfirmPowerOffSettingsMenu : Menu {
    override suspend fun getMenuItem() = Injector.get().getPowerDevicesUseCase().execute(
        GetPowerDevicesUseCase.Params(queryState = false)
    ).map {
        ConfirmPowerOffMenuItem(it.first)
    }


    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___title_confirm_power_off)

    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___subtitle_confirm_power_off)


    class ConfirmPowerOffMenuItem(private val device: PowerDevice) : ToggleMenuItem() {
        private val prefs get() = Injector.get().octoPreferences()
        override val isEnabled get() = prefs.confirmPowerOffDevices.contains(device.uniqueId)
        override val itemId = "confirm_power_off/${device.uniqueId}"
        override var groupId = "devices"
        override val order = 100
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_power_24
        override suspend fun getTitle(context: Context) = device.displayName

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            val base = prefs.confirmPowerOffDevices.toMutableSet()
            if (enabled) base.add(device.uniqueId) else base.remove(device.uniqueId)
            prefs.confirmPowerOffDevices = base
        }
    }
}