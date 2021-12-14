package de.crysxd.octoapp.printcontrols.ui.widget.progress

import android.content.Context
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.RevolvingOptionsMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.ProgressWidgetSettings
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.printcontrols.R
import kotlinx.parcelize.Parcelize

@Parcelize
class ProgressWidgetSettingsMenu : Menu {

    val octoPreferences get() = BaseInjector.get().octoPreferences()
    val context get() = BaseInjector.get().localizedContext()

    override suspend fun getMenuItem() = listOf(
        ShowThumbnailMenuItem(octoPreferences),
        ShowTimeUsedMenuItem(octoPreferences),
        ShowTimeLeftMenuItem(octoPreferences),
        ShowPrinterMessage(octoPreferences),
        EtaStyleMenuItem(octoPreferences, context),
        PrintNameStyleMenuItem(octoPreferences, context),
        FontSizeMenuItem(octoPreferences, context),
        ShowLayerInfoMenuItem(octoPreferences),
        ShowZHeightMenuItem(octoPreferences),
    )

    class FontSizeMenuItem(private val prefs: OctoPreferences, private val context: Context) : RevolvingOptionsMenuItem() {
        override val itemId = "fontSize"
        override var groupId = ""
        override val order = 0
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_format_size_24
        override val activeValue get() = prefs.progressWidgetSettings.fontSize.toString()
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___font_size)
        override val options = ProgressWidgetSettings.FontSize.values().map {
            Option(
                value = it.toString(),
                label = when (it) {
                    ProgressWidgetSettings.FontSize.Small -> context.getString(R.string.progress_widget___settings___font_size_small)
                    ProgressWidgetSettings.FontSize.Normal -> context.getString(R.string.progress_widget___settings___font_size_normal)
                }
            )
        }

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(fontSize = ProgressWidgetSettings.FontSize.valueOf(option.value))
        }
    }

    class PrintNameStyleMenuItem(private val prefs: OctoPreferences, private val context: Context) : RevolvingOptionsMenuItem() {
        override val itemId = "printNameStyle"
        override var groupId = ""
        override val order = 1
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_label_24
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_print_name)
        override val activeValue get() = prefs.progressWidgetSettings.printNameStyle.toString()
        override val options = ProgressWidgetSettings.PrintNameStyle.values().map {
            Option(
                value = it.toString(),
                label = when (it) {
                    ProgressWidgetSettings.PrintNameStyle.None -> context.getString(R.string.progress_widget___settings___show_print_name_none)
                    ProgressWidgetSettings.PrintNameStyle.Compact -> context.getString(R.string.progress_widget___settings___show_print_name_compact)
                    ProgressWidgetSettings.PrintNameStyle.Full -> context.getString(R.string.progress_widget___settings___show_print_name_full)
                }
            )
        }

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(printNameStyle = ProgressWidgetSettings.PrintNameStyle.valueOf(option.value))
        }
    }

    class EtaStyleMenuItem(private val prefs: OctoPreferences, private val context: Context) : RevolvingOptionsMenuItem() {
        override val itemId = "etaStyle"
        override var groupId = "time"
        override val order = 3
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_calendar_today_24
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_eta)
        override val activeValue get() = prefs.progressWidgetSettings.etaStyle.toString()
        override val options = ProgressWidgetSettings.EtaStyle.values().map {
            Option(
                value = it.toString(),
                label = when (it) {
                    ProgressWidgetSettings.EtaStyle.None -> context.getString(R.string.progress_widget___settings___show_eta_none)
                    ProgressWidgetSettings.EtaStyle.Compact -> context.getString(R.string.progress_widget___settings___show_eta_compact)
                    ProgressWidgetSettings.EtaStyle.Full -> context.getString(R.string.progress_widget___settings___show_eta_full)
                }
            )
        }

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(etaStyle = ProgressWidgetSettings.EtaStyle.valueOf(option.value))
        }
    }

    class ShowTimeUsedMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "timeUsed"
        override var groupId = "time"
        override val order = 4
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_restore_24
        override val isChecked get() = prefs.progressWidgetSettings.showUsedTime
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_time_used)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showUsedTime = enabled)
        }
    }

    class ShowTimeLeftMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "timeLeft"
        override var groupId = "time"
        override val order = 5
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_update_24
        override val isChecked get() = prefs.progressWidgetSettings.showLeftTime
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_time_left)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showLeftTime = enabled)
        }
    }

    class ShowLayerInfoMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "layerInfo"
        override var groupId = "gcode"
        override val order = 6
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_layers_24
        override val isChecked get() = prefs.progressWidgetSettings.showLayer
        override fun isVisible(destinationId: Int) = BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW)
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_layer)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showLayer = enabled)
        }
    }

    class ShowZHeightMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "zHeight"
        override var groupId = "gcode"
        override val order = 7
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_height_24
        override val isChecked get() = prefs.progressWidgetSettings.showZHeight
        override fun isVisible(destinationId: Int) = BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW)
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_z_height)
        override fun getDescription(context: Context) = context.getString(R.string.progress_widget___settings___gcode_description)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showZHeight = enabled)
        }
    }


    class ShowThumbnailMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "thumbnail"
        override var groupId = "thumb"
        override val order = 8
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_image_24
        override val isChecked get() = prefs.progressWidgetSettings.showThumbnail
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_thumbnail)
        override fun getDescription(context: Context) = context.getString(R.string.progress_widget___settings___thumbnail_description)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showThumbnail = enabled)
        }
    }

    class ShowPrinterMessage(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "message"
        override var groupId = "message"
        override val order = 10
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_chat_bubble_24
        override val isChecked get() = prefs.progressWidgetSettings.showPrinterMessage
        override fun getTitle(context: Context) = context.getString(R.string.progress_widget___settings___show_printer_message)
        override fun getDescription(context: Context) = "Requires Companion plugin"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showPrinterMessage = enabled)
        }
    }
}