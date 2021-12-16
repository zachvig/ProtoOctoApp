package de.crysxd.baseui.menu.power

import android.content.Context
import android.os.Parcelable
import de.crysxd.baseui.R
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.menu.ConfirmedMenuItem
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.SubMenuItem
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_POWER_DEVICE_CYCLE
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_POWER_DEVICE_OFF
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_POWER_DEVICE_ON
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_POWER_DEVICE_TOGGLE
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SHOW_POWER_DEVICE_ACTIONS
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.ext.urlDecode
import de.crysxd.octoapp.base.ext.urlEncode
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

@Parcelize
class PowerControlsMenu(val type: DeviceType = DeviceType.Unspecified, val action: Action = Action.Unspecified) : Menu {

    override suspend fun shouldShowMenu(host: MenuHost): Boolean {
        // Let's try to solve the task at hand without the user selecting somehting
        val start = System.currentTimeMillis()
        val allDevices = BaseInjector.get().getPowerDevicesUseCase().execute(
            GetPowerDevicesUseCase.Params(
                queryState = false,
                requiredCapabilities = type.requiredCapabilities
            )
        )

        // Is there a default device the user told us to always use?
        val defaultDevice = when (type) {
            DeviceType.PrinterPsu -> BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.appSettings?.defaultPowerDevices?.get(type.prefKey)
            DeviceType.Light -> BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.appSettings?.defaultPowerDevices?.get(type.prefKey)
            DeviceType.Unspecified -> null
        }?.let { id ->
            allDevices.firstOrNull { it.first.uniqueId == id }?.first
        }

        // Is there only one device?
        val onlyDevice = allDevices.firstOrNull()?.takeIf { allDevices.size == 1 }?.first
        val deviceToUse = defaultDevice ?: onlyDevice

        return if (action != Action.Unspecified && deviceToUse != null) {
            // We already know what to do!
            when (action) {
                Action.TurnOn -> BaseInjector.get().turnOnPsuUseCase().execute(deviceToUse)
                Action.TurnOff -> BaseInjector.get().turnOffPsuUseCase().execute(deviceToUse)
                Action.Cycle -> BaseInjector.get().cyclePsuUseCase().execute(deviceToUse)
                Action.Toggle -> BaseInjector.get().togglePsuUseCase().execute(deviceToUse)
                Action.Unspecified -> Unit
            }

            // Don't flash up. Load at least 500ms
            val minDurationDelay = 750 - (System.currentTimeMillis() - start)
            if (minDurationDelay > 0) {
                delay(minDurationDelay)
            }

            host.handleAction(action, type, deviceToUse)
            false
        } else {
            true
        }
    }

    override fun getEmptyStateIcon() = R.drawable.octo_power_devices
    override fun getEmptyStateActionText(context: Context) = context.getString(R.string.power_menu___empty_state_action)
    override fun getEmptyStateActionUrl(context: Context) = UriLibrary.getPluginLibraryUri(category = "power").toString()
    override fun getCheckBoxText(context: Context) =
        context.getString(R.string.power_menu___checkbox_label).takeIf { type != DeviceType.Unspecified && action != Action.Unspecified }

    override fun getEmptyStateSubtitle(context: Context) =
        context.getString(R.string.power_menu___empty_state_subtitle)

    override suspend fun getMenuItem() = BaseInjector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(queryState = false))
        .mapNotNull {
            val name = it.first.displayName
            val id = it.first.uniqueId
            val pluginName = it.first.pluginDisplayName
            val toggle = TogglePowerDeviceMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
                .takeIf { _ -> it.first.controlMethods.contains(PowerDevice.ControlMethod.Toggle) }

            when (action) {
                Action.Unspecified -> ShowPowerDeviceActionsMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name)
                Action.Cycle -> CyclePowerDeviceMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
                Action.TurnOff -> TurnPowerDeviceOffMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
                    .takeIf { _ -> it.first.controlMethods.contains(PowerDevice.ControlMethod.TurnOnOff) } ?: toggle
                Action.TurnOn -> TurnPowerDeviceOnMenuItem(uniqueDeviceId = id, pluginName = pluginName, name = name, showName = true, deviceType = type)
                    .takeIf { _ -> it.first.controlMethods.contains(PowerDevice.ControlMethod.TurnOnOff) } ?: toggle
                Action.Toggle -> toggle
            }
        }

    override suspend fun getTitle(context: Context) = when (type) {
        DeviceType.Unspecified -> context.getString(R.string.power_menu___title_neutral)
        DeviceType.Light -> context.getString(R.string.power_menu___title_lights)
        DeviceType.PrinterPsu -> context.getString(R.string.power_menu___title_select_device)
    }

    override fun getBottomText(context: Context) =
        context.getString(R.string.power_menu___bottom_text).toHtml()

    override fun getBottomMovementMethod(host: MenuHost) =
        LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.getMenuActivity()))

    companion object {
        private suspend fun MenuHost.handleAction(action: Action, deviceType: DeviceType, device: PowerDevice) {
            if (isCheckBoxChecked()) {
                BaseInjector.get().octorPrintRepository().updateAppSettingsForActive {
                    it.copy(
                        defaultPowerDevices = (it.defaultPowerDevices ?: emptyMap())
                            .toMutableMap().apply {
                                this[deviceType.prefKey] = device.uniqueId
                            }
                    )
                }
            }

            (getHostFragment() as? PowerControlsCallback)?.onPowerActionCompleted(action, device)?.let {
                closeMenu()
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
        override fun getTitle(context: Context) = name
    }

    class TurnPowerDeviceOffMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val deviceType: DeviceType = DeviceType.Unspecified,
    ) : ConfirmedMenuItem() {
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
        private val needsConfirmation = BaseInjector.get().octoPreferences().confirmPowerOffDevices.contains(uniqueDeviceId)

        override fun getTitle(context: Context) =
            if (showName) context.getString(R.string.power_menu___turn_x_off, name) else context.getString(R.string.power_menu___turn_off)


        override fun getConfirmMessage(context: Context) = context.getString(R.string.power_menu___confirm_turn_x_off, name)
        override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.power_menu___turn_x_off, name)

        override suspend fun onConfirmed(host: MenuHost?) {
            val device = BaseInjector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            BaseInjector.get().turnOffPsuUseCase().execute(device)
            host?.handleAction(Action.TurnOff, deviceType, device)
        }

        override suspend fun onClicked(host: MenuHost?) {
            if (needsConfirmation) {
                super.onClicked(host)
            } else {
                onConfirmed(host)
            }
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

        override fun getTitle(context: Context) =
            if (showName) context.getString(R.string.power_menu___turn_x_on, name) else context.getString(R.string.power_menu___turn_on)

        override suspend fun onClicked(host: MenuHost?) {
            val device = BaseInjector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            BaseInjector.get().turnOnPsuUseCase().execute(device)
            host?.handleAction(Action.TurnOn, deviceType, device)
        }
    }

    class TogglePowerDeviceMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val deviceType: DeviceType = DeviceType.Unspecified,
    ) : ConfirmedMenuItem() {
        companion object {
            fun forItemId(itemId: String): TogglePowerDeviceMenuItem {
                val parts = itemId.split("/")
                return TogglePowerDeviceMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val itemId = "$MENU_ITEM_POWER_DEVICE_TOGGLE/$uniqueDeviceId/${name.urlEncode()}/${pluginName.urlEncode()}"
        override var groupId = ""
        override val order = 335
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_power_cycle_24px
        private val needsConfirmation = BaseInjector.get().octoPreferences().confirmPowerOffDevices.contains(uniqueDeviceId)

        override fun getTitle(context: Context) =
            if (showName) context.getString(R.string.power_menu___toggle_x, name) else context.getString(R.string.power_menu___toggle)

        override fun getConfirmMessage(context: Context) = context.getString(R.string.power_menu___confirm_toggle_x, name)
        override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.power_menu___toggle_x, name)

        override suspend fun onConfirmed(host: MenuHost?) {
            val device = BaseInjector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            BaseInjector.get().togglePsuUseCase().execute(device)
            host?.handleAction(Action.Toggle, deviceType, device)
        }

        override suspend fun onClicked(host: MenuHost?) {
            if (needsConfirmation) {
                super.onClicked(host)
            } else {
                onConfirmed(host)
            }
        }
    }

    class CyclePowerDeviceMenuItem(
        val uniqueDeviceId: String,
        val name: String,
        val pluginName: String,
        val showName: Boolean = true,
        val deviceType: DeviceType = DeviceType.Unspecified,
    ) : ConfirmedMenuItem() {
        companion object {
            fun forItemId(itemId: String): CyclePowerDeviceMenuItem {
                val parts = itemId.split("/")
                return CyclePowerDeviceMenuItem(parts[1].urlDecode(), parts[2].urlDecode(), parts[3].urlDecode())
            }
        }

        override val itemId = "$MENU_ITEM_POWER_DEVICE_CYCLE/$uniqueDeviceId/${name.urlEncode()}/${pluginName.urlEncode()}"
        override var groupId = ""
        override val order = 336
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_power_cycle_24px
        private val needsConfirmation = BaseInjector.get().octoPreferences().confirmPowerOffDevices.contains(uniqueDeviceId)

        override fun getTitle(context: Context) =
            if (showName) context.getString(R.string.power_menu___cycle_x, name) else context.getString(R.string.power_menu___cycle)


        override fun getConfirmMessage(context: Context) = context.getString(R.string.power_menu___confirm_cycle_x, name)
        override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.power_menu___cycle_x, name)

        override suspend fun onConfirmed(host: MenuHost?) {
            val device = BaseInjector.get().getPowerDevicesUseCase().execute(
                GetPowerDevicesUseCase.Params(
                    queryState = false, onlyGetDeviceWithUniqueId = uniqueDeviceId
                )
            ).first().first

            BaseInjector.get().cyclePsuUseCase().execute(device)
            host?.handleAction(Action.Cycle, deviceType, device)
        }

        override suspend fun onClicked(host: MenuHost?) {
            if (needsConfirmation) {
                super.onClicked(host)
            } else {
                onConfirmed(host)
            }
        }
    }

    interface PowerControlsCallback {
        fun onPowerActionCompleted(action: Action, device: PowerDevice)
    }

    sealed class DeviceType : Parcelable {
        val prefKey get() = this::class.java.simpleName.lowercase()
        abstract val requiredCapabilities: List<PowerDevice.Capability>

        @Parcelize
        object PrinterPsu : DeviceType() {
            override val requiredCapabilities get() = listOf(PowerDevice.Capability.ControlPrinterPower)
        }

        @Parcelize
        object Light : DeviceType() {
            override val requiredCapabilities get() = listOf(PowerDevice.Capability.Illuminate)
        }

        @Parcelize
        object Unspecified : DeviceType() {
            override val requiredCapabilities get() = emptyList<PowerDevice.Capability>()
        }
    }

    sealed class Action : Parcelable {
        @Parcelize
        object TurnOn : Action()

        @Parcelize
        object Toggle : Action()

        @Parcelize
        object TurnOff : Action()

        @Parcelize
        object Cycle : Action()

        @Parcelize
        object Unspecified : Action()
    }
}