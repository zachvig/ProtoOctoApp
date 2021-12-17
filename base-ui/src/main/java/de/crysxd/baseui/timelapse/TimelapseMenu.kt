package de.crysxd.baseui.timelapse

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.RevolvingOptionsMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseConfig
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat

@Parcelize
class TimelapseMenu : Menu {

    override suspend fun getTitle(context: Context) = "Timelapse**"

    override suspend fun getMenuItem(): List<MenuItem> {
        val config = BaseInjector.get().timelapseRepository().fetchLatest().config
        requireNotNull(config)
        return listOf(
            AskForTimelapseBeforePrintingMenuItem(),
            TimelapseModeMenuItem(config),
            TimelapseMinimumIntervalMenuItem(config),
            TimelapseZHopMenuItem(config),
            TimelapseFrameRateMenuItem(config),
            TimelapsePostRollMenuItem(config),
            TimelapseIntervalItem(config)
        )
    }

    class AskForTimelapseBeforePrintingMenuItem : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().askForTimelapseBeforePrinting
        override val itemId = "timelapse_ask_before_printing"
        override var groupId = "prep"
        override val order = 261
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_control_camera_24
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getTitle(context: Context) = "Ask before printing**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().askForTimelapseBeforePrinting = enabled
        }
    }

    class TimelapseModeMenuItem(private val config: TimelapseConfig) : RevolvingOptionsMenuItem() {
        override val itemId = "timelapse_mode"
        override var groupId = "settings"
        override val order = 262
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_videocam_24
        override val activeValue get() = (config.type ?: TimelapseConfig.Type.Off).name
        override val options = listOf(
            Option(label = "Off**", value = TimelapseConfig.Type.Off.name),
            Option(label = "Timed**", value = TimelapseConfig.Type.Timed.name),
            Option(label = "On Z change**", value = TimelapseConfig.Type.ZChange.name),
        )

        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getTitle(context: Context) = "Mode**"
        override suspend fun handleOptionActivated(host: MenuHost?, option: Option) {
            BaseInjector.get().timelapseRepository().update {
                copy(type = TimelapseConfig.Type.valueOf(option.value))
            }
            host?.reloadMenu()
        }
    }

    class TimelapseMinimumIntervalMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_minimum_interval"
        override var groupId = "settings"
        override val order = 263
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_access_time_24

        override fun getTitle(context: Context) = "Minimum interval**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint && config.type == TimelapseConfig.Type.ZChange
        override fun getRightDetail(context: Context) = config.minDelay?.let { context.getString(R.string.x_secs, it) }
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(minDelay = 44)
            }
            host?.reloadMenu()
        }
    }

    class TimelapseIntervalItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_interval"
        override var groupId = "settings"
        override val order = 263
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_access_time_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Interval**"
        override fun isVisible(destinationId: Int) = config.type == TimelapseConfig.Type.Timed && destinationId == R.id.workspacePrePrint
        override fun getRightDetail(context: Context) = config.interval?.let { context.getString(R.string.x_secs, it) }
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(interval = 44)
            }
            host?.reloadMenu()
        }
    }

    class TimelapseZHopMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_z_hop"
        override var groupId = "settings"
        override val order = 264
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_height_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Retraction Z-Hop**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint && config.type == TimelapseConfig.Type.ZChange
        override fun getRightDetail(context: Context) = config.retractionZHop?.let { context.getString(R.string.x_mm, NumberFormat.getInstance().format(it)) }
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(retractionZHop = 44.4f)
            }
            host?.reloadMenu()
        }
    }

    class TimelapseFrameRateMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_frame_rate"
        override var groupId = "settings"
        override val order = 265
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_animation_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Frame rate**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun isEnabled(destinationId: Int) = config.type != TimelapseConfig.Type.Off
        override fun getRightDetail(context: Context) = config.fps?.let { context.getString(R.string.x_fps, it) }
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(fps = 44)
            }
            host?.reloadMenu()
        }
    }

    class TimelapsePostRollMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_post_roll"
        override var groupId = "settings"
        override val order = 266
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_update_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Post roll**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun isEnabled(destinationId: Int) = config.type != TimelapseConfig.Type.Off
        override fun getRightDetail(context: Context) = config.postRoll?.let { context.getString(R.string.x_secs, it) }
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(postRoll = 44)
            }
            host?.reloadMenu()
        }
    }
}