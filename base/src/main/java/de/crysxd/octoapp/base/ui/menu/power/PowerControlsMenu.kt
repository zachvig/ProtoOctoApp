package de.crysxd.octoapp.base.ui.menu.power

import android.content.Context
import android.os.Parcelable
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
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
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
class PowerControlsMenu(val type: DeviceType = DeviceType.Unspecified, val action: Action = Action.Unspecified) : Menu {

    override suspend fun beforeShow(host: MenuBottomSheetFragment): Boolean {
        // Let's try to solve the task at hand without the user selecting somehting

        val deviceToUse = when (type) {
            DeviceType.PrinterPsu -> Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.appSettings?.defaultPowerDevices?.get(type.prefKey)
            DeviceType.Unspecified -> null
        }?.let {
            Injector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = false, onlyGetDeviceWithUniqueId = it)).first().first
        }

        return if (action != Action.Unspecified && deviceToUse != null) {
            // We already know what to do!
            when (action) {
                Action.TurnOn -> Injector.get().turnOnPsuUseCase().execute(deviceToUse)
                Action.TurnOff -> Injector.get().turnOffPsuUseCase().execute(deviceToUse)
                Action.Cycle -> Injector.get().cyclePsuUseCase().execute(deviceToUse)
                Action.Unspecified -> Unit
            }
            host.handleAction(action, type, deviceToUse)
            true
        } else {
            false
        }
    }

    override fun getEmptyStateIcon() = R.drawable.octo_power_devices
    override fun getEmptyStateActionText(context: Context) = "Learn more"
    override fun getEmptyStateActionUrl(context: Context) = Firebase.remoteConfig.getString("help_url_power_devices")
    override fun getCheckBoxText(context: Context) = "Always use this device in the future".takeIf { type != DeviceType.Unspecified && action != Action.Unspecified }
    override fun getEmptyStateSubtitle(context: Context) =
        "OctoApp can control your printer’s power supply and other devices with supported plugins. Once set up, they will show up here!"

    override suspend fun getMenuItem() = Injector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = false))
        .map {
            val name = it.first.displayName
            val id = it.first.uniqueId
            val pluginName = it.first.pluginDisplayName

            when (action) {
                Action.Unspecified -> ShowPowerDeviceActionsMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name)
                Action.Cycle -> CyclePowerDeviceMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
                Action.TurnOff -> TurnPowerDeviceOffMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
                Action.TurnOn -> TurnPowerDeviceOnMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
            }
        }

    override suspend fun getTitle(context: Context) = when (type) {
        DeviceType.Unspecified -> "Power devices"
        DeviceType.PrinterPsu -> "Which device is the printer?"
    }

    override fun getBottomText(context: Context) =
        "<small>Power devices are provided by a <a href=\"https://google.com\">supported plugin</a></small>".toHtml()

    override fun getBottomMovementMethod(host: MenuBottomSheetFragment) =
        LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener())

    companion object {
        private suspend fun MenuBottomSheetFragment.handleAction(action: Action, deviceType: DeviceType, device: PowerDevice) {
            (parentFragment as? PowerControlsCallback)?.onPowerActionCompleted(action, device)
            if (isCheckBoxChecked) {
                Injector.get().octorPrintRepository().updateAppSettingsForActive {
                    it.copy(
                        defaultPowerDevices = (it.defaultPowerDevices ?: emptyMap()).toMutableMap().apply { this[deviceType.prefKey] = device.uniqueId })
                }
            }
        }
    }

    class ShowPowerDeviceActionsMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
    ) : SubMenuItem() {
        companion object {
            fun forItemId(itemId: String): ShowPowerDeviceActionsMenuItem {
                val parts = itemId.split("/")
                return ShowPowerDeviceActionsMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val subMenu get() = PowerDeviceMenu(uniqueDeviceId = uniqueDeviceId, name = name, pluginName = pluginName)
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
        val deviceType: DeviceType = DeviceType.Unspecified,
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

        override suspend fun getTitle(context: Context) = if (showName) name else "Turn off"
        override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
            val device = Injector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            executeAsync {
                Injector.get().turnOffPsuUseCase().execute(device)
            }

            host.handleAction(Action.TurnOff, deviceType, device)
            return true
        }
    }

    class TurnPowerDeviceOnMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val deviceType: DeviceType = DeviceType.Unspecified,
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
            val device = Injector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            executeAsync {
                Injector.get().turnOnPsuUseCase().execute(device)
            }

            host.handleAction(Action.TurnOn, deviceType, device)
            return true
        }
    }

    class CyclePowerDeviceMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val deviceType: DeviceType = DeviceType.Unspecified,
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
            val device = Injector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            executeAsync {
                Injector.get().cyclePsuUseCase().execute(device)
            }

            host.handleAction(Action.Cycle, deviceType, device)
            return true
        }
    }

    interface PowerControlsCallback {
        fun onPowerActionCompleted(action: Action, device: PowerDevice)
    }

    sealed class DeviceType : Parcelable {
        val prefKey get() = this::class.java.simpleName.toLowerCase(Locale.ENGLISH)

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