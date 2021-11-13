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
    override suspend fun getMenuItem() = listOf(
        ShowThumbnailMenuItem(octoPreferences),
        ShowTimeUsedMenuItem(octoPreferences),
        ShowTimeLeftMenuItem(octoPreferences),
        EtaStyleMenuItem(octoPreferences),
        PrintNameStyleMenuItem(octoPreferences),
        FontSizeMenuItem(octoPreferences),
        ShowLayerInfoMenuItem(octoPreferences),
        ShowZHeightMenuItem(octoPreferences),
    )

    class FontSizeMenuItem(private val prefs: OctoPreferences) : RevolvingOptionsMenuItem() {
        override val itemId = "fontSize"
        override var groupId = ""
        override val order = 0
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_format_size_24
        override val activeValue get() = prefs.progressWidgetSettings.fontSize.toString()
        override suspend fun getTitle(context: Context) = "Font size**"
        override val options = ProgressWidgetSettings.FontSize.values().map {
            Option(
                value = it.toString(),
                label = when (it) {
                    ProgressWidgetSettings.FontSize.Small -> "Small**"
                    ProgressWidgetSettings.FontSize.Normal -> "Normal**"
                }
            )
        }

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(fontSize = ProgressWidgetSettings.FontSize.valueOf(option.value))
        }
    }

    class PrintNameStyleMenuItem(private val prefs: OctoPreferences) : RevolvingOptionsMenuItem() {
        override val itemId = "printNameStyle"
        override var groupId = ""
        override val order = 1
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_label_24
        override suspend fun getTitle(context: Context) = "Show print name**"
        override val activeValue get() = prefs.progressWidgetSettings.printNameStyle.toString()
        override val options = ProgressWidgetSettings.PrintNameStyle.values().map {
            Option(
                value = it.toString(),
                label = when (it) {
                    ProgressWidgetSettings.PrintNameStyle.None -> "None**"
                    ProgressWidgetSettings.PrintNameStyle.Compact -> "Compact**"
                    ProgressWidgetSettings.PrintNameStyle.Full -> "Full**"
                }
            )
        }

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(printNameStyle = ProgressWidgetSettings.PrintNameStyle.valueOf(option.value))
        }
    }

    class EtaStyleMenuItem(private val prefs: OctoPreferences) : RevolvingOptionsMenuItem() {
        override val itemId = "etaStyle"
        override var groupId = "time"
        override val order = 2
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_calendar_today_24
        override suspend fun getTitle(context: Context) = "Show ETA**"
        override val activeValue get() = prefs.progressWidgetSettings.etaStyle.toString()
        override val options = ProgressWidgetSettings.EtaStyle.values().map {
            Option(
                value = it.toString(),
                label = when (it) {
                    ProgressWidgetSettings.EtaStyle.None -> "None**"
                    ProgressWidgetSettings.EtaStyle.Compact -> "Compact**"
                    ProgressWidgetSettings.EtaStyle.Full -> "Full**"
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
        override val order = 3
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_restore_24
        override val isChecked get() = prefs.progressWidgetSettings.showUsedTime
        override suspend fun getTitle(context: Context) = "Show used time**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showUsedTime = enabled)
        }
    }

    class ShowTimeLeftMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "timeLeft"
        override var groupId = "time"
        override val order = 4
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_update_24
        override val isChecked get() = prefs.progressWidgetSettings.showLeftTime
        override suspend fun getTitle(context: Context) = "Show time left**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showLeftTime = enabled)
        }
    }

    class ShowLayerInfoMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "layerInfo"
        override var groupId = "gcode"
        override val order = 5
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_layers_24
        override val isChecked get() = prefs.progressWidgetSettings.showLayer
        override suspend fun isVisible(destinationId: Int) = BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW)
        override suspend fun getTitle(context: Context) = "Show layer information**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showLayer = enabled)
        }
    }

    class ShowZHeightMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "zHeight"
        override var groupId = "gcode"
        override val order = 6
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_height_24
        override val isChecked get() = prefs.progressWidgetSettings.showZHeight
        override suspend fun isVisible(destinationId: Int) = BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW)
        override suspend fun getTitle(context: Context) = "Show Z Height**"
        override suspend fun getDescription(context: Context) =
            "Synced with the Gcode Preview, if text is 'Unavailable' or 'Large file', check the Gcode Preview for details**"

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showZHeight = enabled)
        }
    }


    class ShowThumbnailMenuItem(private val prefs: OctoPreferences) : ToggleMenuItem() {
        override val itemId = "thumbnail"
        override var groupId = "thumb"
        override val order = 7
        override val canBePinned = false
        override val style = MenuItemStyle.Settings
        override val icon: Int = R.drawable.ic_round_image_24
        override val isChecked get() = prefs.progressWidgetSettings.showThumbnail
        override suspend fun getTitle(context: Context) = "Show thumbnail**"
        override suspend fun getDescription(context: Context) = "Thumbnails are generated by the Cura and Prusa Slicer plugins and only shown if available**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            prefs.progressWidgetSettings = prefs.progressWidgetSettings.copy(showThumbnail = enabled)
        }
    }
}