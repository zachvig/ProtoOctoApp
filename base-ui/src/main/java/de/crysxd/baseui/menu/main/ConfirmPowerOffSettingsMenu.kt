package de.crysxd.baseui.menu.main

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.parcelize.Parcelize

@Parcelize
class ConfirmPowerOffSettingsMenu : Menu {
    override suspend fun getMenuItem() = BaseInjector.get().getPowerDevicesUseCase().execute(
        GetPowerDevicesUseCase.Params(queryState = false)
    ).map {
        ConfirmPowerOffMenuItem(it.first)
    }


    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___title_confirm_power_off)

    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___subtitle_confirm_power_off)


    class ConfirmPowerOffMenuItem(private val device: PowerDevice) : ToggleMenuItem() {
        private val prefs get() = BaseInjector.get().octoPreferences()
        override val isChecked get() = prefs.confirmPowerOffDevices.contains(device.uniqueId)
        override val itemId = "confirm_power_off/${device.uniqueId}"
        override var groupId = "devices"
        override val order = 100
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_power_24
        override fun getTitle(context: Context) = device.displayName

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            val base = prefs.confirmPowerOffDevices.toMutableSet()
            if (enabled) base.add(device.uniqueId) else base.remove(device.uniqueId)
            prefs.confirmPowerOffDevices = base
        }
    }
}