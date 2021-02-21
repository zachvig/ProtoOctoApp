package de.crysxd.octoapp.base.ui.menu.power

import android.content.Context
import android.os.Parcelable
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.ext.urlDecode
import de.crysxd.octoapp.base.ext.urlEncode
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_POWER_DEVICE_CYCLE
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_POWER_DEVICE_OFF
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_POWER_DEVICE_ON
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_SHOW_POWER_DEVICE_ACTIONS
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import kotlinx.android.parcel.Parcelize

@Parcelize
class PowerControlsMenu(val type: DeviceType = DeviceType.Unspecified, val action: Action = Action.Unspecified) : Menu {

    override suspend fun getMenuItem() = Injector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = true)).map {
        val name = it.first.displayName
        val id = it.first.uniqueId
        val pluginName = it.first.pluginDisplayName
        val state = it.second

        when (action) {
            Action.Unspecified -> ShowPowerDeviceActionsMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, state = state)
            Action.Cycle -> CyclePowerDeviceMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, state = state, showName = true)
            Action.TurnOff -> TurnPowerDeviceOffMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, state = state, showName = true)
            Action.TurnOn -> TurnPowerDeviceOnMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, state = state, showName = true)
        }
    }

    override fun getTitle(context: Context) = when (action) {
        Action.TurnOn -> "Select Device to Turn On"
        Action.TurnOff -> "Select Device to Turn Off"
        Action.Cycle -> "Select Device to Cycle"
        Action.Unspecified -> "Power devices"
    }

    override fun getBottomText(context: Context) = "<small>Power devices are provided by a <a href=\"https://google.com\">supported plugin</a></small>".toHtml()
    override fun getBottomMovementMethod(host: MenuBottomSheetFragment) = LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener())

    class ShowPowerDeviceActionsMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val state: GetPowerDevicesUseCase.PowerState = GetPowerDevicesUseCase.PowerState.Unknown
    ) : SubMenuItem() {
        companion object {
            fun forItemId(itemId: String): ShowPowerDeviceActionsMenuItem {
                val parts = itemId.split("/")
                return ShowPowerDeviceActionsMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val subMenu get() = PowerDeviceMenu(uniqueDeviceId = uniqueDeviceId, name = name, pluginName = pluginName, powerState = state)
        override val itemId = "$MENU_ITEM_SHOW_POWER_DEVICE_ACTIONS/$uniqueDeviceId/${name.urlEncode()}/${pluginName.urlEncode()}"
        override var groupId = ""
        override val order = 332
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_power_settings_new_24
        override suspend fun getTitle(context: Context) = name
    }

    class TurnPowerDeviceOffMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val state: GetPowerDevicesUseCase.PowerState = GetPowerDevicesUseCase.PowerState.Unknown
    ) : MenuItem {
        companion object {
            fun forItemId(itemId: String): TurnPowerDeviceOffMenuItem {
                val parts = itemId.split("/")
                return TurnPowerDeviceOffMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val itemId = "$MENU_ITEM_POWER_DEVICE_OFF/$uniqueDeviceId/${name.urlEncode()}/${pluginName.urlEncode()}"
        override var groupId = ""
        override val order = 333
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_power_off_24

        override suspend fun getTitle(context: Context) = if (showName) "Turn $name off" else "Turn off"
        override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
            executeAsync {
                val device = Injector.get().getPowerDevicesUseCase().execute(
                    GetPowerDevicesUseCase.Params(
                        queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                    )
                ).first().first
                Injector.get().turnOffPsuUseCase().execute(device)
            }
            return true
        }
    }

    class TurnPowerDeviceOnMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val state: GetPowerDevicesUseCase.PowerState = GetPowerDevicesUseCase.PowerState.Unknown
    ) : MenuItem {
        companion object {
            fun forItemId(itemId: String): TurnPowerDeviceOnMenuItem {
                val parts = itemId.split("/")
                return TurnPowerDeviceOnMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val itemId = "$MENU_ITEM_POWER_DEVICE_ON/$uniqueDeviceId/${name.urlEncode()}/${pluginName.urlEncode()}"
        override var groupId = ""
        override val order = 334
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_power_24

        override suspend fun getTitle(context: Context) = if (showName) "Turn $name on" else "Turn on"
        override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
            executeAsync {
                val device = Injector.get().getPowerDevicesUseCase().execute(
                    GetPowerDevicesUseCase.Params(
                        queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                    )
                ).first().first
                Injector.get().turnOnPsuUseCase().execute(device)
            }
            return true
        }
    }

    class CyclePowerDeviceMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val state: GetPowerDevicesUseCase.PowerState = GetPowerDevicesUseCase.PowerState.Unknown
    ) : MenuItem {
        companion object {
            fun forItemId(itemId: String): CyclePowerDeviceMenuItem {
                val parts = itemId.split("/")
                return CyclePowerDeviceMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val itemId = "$MENU_ITEM_POWER_DEVICE_CYCLE/$uniqueDeviceId/${name.urlEncode()}/${pluginName.urlEncode()}"
        override var groupId = ""
        override val order = 335
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_power_cycle_24px

        override suspend fun getTitle(context: Context) = if (showName) "Cycle $name" else "Cycle"
        override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
            executeAsync {
                val device = Injector.get().getPowerDevicesUseCase().execute(
                    GetPowerDevicesUseCase.Params(
                        queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                    )
                ).first().first
                Injector.get().cyclePsuUseCase().execute(device)
            }
            return true
        }
    }


    sealed class DeviceType : Parcelable {
        @Parcelize
        object PrinterPsu : DeviceType()

        @Parcelize
        object Unspecified : DeviceType()
    }

    sealed class Action : Parcelable {
        @Parcelize
        object TurnOn : Action()

        @Parcelize
        object TurnOff : Action()

        @Parcelize
        object Cycle : Action()

        @Parcelize
        object Unspecified : Action()
    }
}