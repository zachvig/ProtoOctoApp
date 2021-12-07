package de.crysxd.baseui.common.gcode

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.RevolvingOptionsMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.data.models.GcodePreviewSettings
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.parcelize.Parcelize

@Parcelize
class GcodeSettingsMenu : Menu {

    override suspend fun getMenuItem() = listOf(
        ShowPreviousLayer(),
        ShowCurrentLayer(),
        Quality(),
    )

    class ShowPreviousLayer : ToggleMenuItem() {
        private val preferences = BaseInjector.get().octoPreferences()
        override val isEnabled get() = preferences.gcodePreviewSettings.showPreviousLayer
        override val itemId = "previous"
        override var groupId = "none"
        override val order = 0
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_layers_24

        override fun getTitle(context: Context) = context.getString(R.string.gcode_preview_settings___show_previous_layer)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            preferences.gcodePreviewSettings = preferences.gcodePreviewSettings.copy(showPreviousLayer = enabled)
            host.reloadMenu()
        }
    }

    class ShowCurrentLayer : ToggleMenuItem() {
        private val preferences = BaseInjector.get().octoPreferences()
        override val isEnabled get() = preferences.gcodePreviewSettings.showCurrentLayer
        override val itemId = "current"
        override var groupId = "none"
        override val order = 1
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_layers_24

        override fun getTitle(context: Context) = context.getString(R.string.gcode_preview_settings___show_current_layer)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            preferences.gcodePreviewSettings = preferences.gcodePreviewSettings.copy(showCurrentLayer = enabled)
        }
    }

    class Quality : RevolvingOptionsMenuItem() {
        private val preferences = BaseInjector.get().octoPreferences()
        private val context = BaseInjector.get().localizedContext()
        override val activeValue get() = preferences.gcodePreviewSettings.quality.name
        override val options = listOf(
            Option(label = context.getString(R.string.gcode_preview_settings___quality_low), value = GcodePreviewSettings.Quality.Low.name),
            Option(label = context.getString(R.string.gcode_preview_settings___quiality_medium), value = GcodePreviewSettings.Quality.Medium.name),
            Option(label = context.getString(R.string.gcode_preview_settings___quality_ultra), value = GcodePreviewSettings.Quality.Ultra.name)
        )
        override val isEnabled = true
        override val itemId = "quality"
        override var groupId = "none"
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_speed_24

        override fun getTitle(context: Context) = context.getString(R.string.gcode_preview_settings___quality)
        override fun getDescription(context: Context) = when (BaseInjector.get().octoPreferences().gcodePreviewSettings.quality) {
            GcodePreviewSettings.Quality.Low -> context.getString(R.string.gcode_preview_settings___quality_low_description)
            GcodePreviewSettings.Quality.Medium -> context.getString(R.string.gcode_preview_settings___quality_medium_description)
            GcodePreviewSettings.Quality.Ultra -> context.getString(R.string.gcode_preview_settings___quality_ultra_description)
        }

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            preferences.gcodePreviewSettings = preferences.gcodePreviewSettings.copy(quality = GcodePreviewSettings.Quality.valueOf(option.value))
        }
    }
}