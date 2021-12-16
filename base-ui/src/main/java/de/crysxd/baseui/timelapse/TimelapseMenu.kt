package de.crysxd.baseui.timelapse

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.RevolvingOptionsMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import kotlinx.parcelize.Parcelize

@Parcelize
class TimelapseMenu : Menu {

    companion object {
        private var mode: String = "off"
    }

    override suspend fun getTitle(context: Context) = "Timelapse**"
    override suspend fun getSubtitle(context: Context) = "You can configure a timelapse before starting a print."

    override suspend fun getMenuItem() = listOf(
        AskForTimelapseBeforePrintingMenuItem(),
        TimelapseModeMenuItem(),
        TimelapseMinimumIntervalMenuItem(),
        TimelapseZHopMenuItem(),
        TimelapseFrameRateMenuItem(),
        TimelapsePostRollMenuItem(),
        TimelapseIntervalItem(),
        TimelapseArchiveMenuItem()
    )

    class AskForTimelapseBeforePrintingMenuItem : ToggleMenuItem() {
        override val isChecked = true
        override val itemId = "timelapse_ask_before_printing"
        override var groupId = "prep"
        override val order = 261
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_control_camera_24
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getTitle(context: Context) = "Ask before printing**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            TODO("Not yet implemented")
        }
    }

    class TimelapseModeMenuItem : RevolvingOptionsMenuItem() {
        override val itemId = "timelapse_mode"
        override var groupId = "settings"
        override val order = 262
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_videocam_24
        override val activeValue get() = mode
        override val options = listOf(
            Option(label = "Off**", value = "off"),
            Option(label = "Timed**", value = "timed"),
            Option(label = "On Z change**", value = "zchange"),
        )

        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getTitle(context: Context) = "Mode**"
        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            mode = option.value
            host?.reloadMenu()
        }
    }

    class TimelapseMinimumIntervalMenuItem : MenuItem {
        override val itemId = "timelapse_minimum_interval"
        override var groupId = "settings"
        override val order = 263
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_access_time_24

        override fun getTitle(context: Context) = "Minimum interval**"
        override fun isVisible(destinationId: Int) = mode == "zchange"
        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getRightDetail(context: Context) = "5s"
        override suspend fun onClicked(host: MenuHost?) {
            TODO("Not yet implemented")
        }
    }

    class TimelapseIntervalItem : MenuItem {
        override val itemId = "timelapse_interval"
        override var groupId = "settings"
        override val order = 263
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_access_time_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Interval**"
        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun isVisible(destinationId: Int) = mode == "timed"
        override fun getRightDetail(context: Context) = "10s"
        override suspend fun onClicked(host: MenuHost?) {
            TODO("Not yet implemented")
        }
    }

    class TimelapseZHopMenuItem : MenuItem {
        override val itemId = "timelapse_z_hop"
        override var groupId = "settings"
        override val order = 264
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_height_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Retraction Z-Hop**"
        override fun isVisible(destinationId: Int) = mode == "zchange"
        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getRightDetail(context: Context) = "5s"
        override suspend fun onClicked(host: MenuHost?) {
            TODO("Not yet implemented")
        }
    }

    class TimelapseFrameRateMenuItem : MenuItem {
        override val itemId = "timelapse_frame_rate"
        override var groupId = "settings"
        override val order = 265
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_animation_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Frame rate**"
        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint && mode != "off"
        override fun getRightDetail(context: Context) = "25 FPS"
        override suspend fun onClicked(host: MenuHost?) {
            TODO("Not yet implemented")
        }
    }

    class TimelapsePostRollMenuItem : MenuItem {
        override val itemId = "timelapse_post_roll"
        override var groupId = "settings"
        override val order = 266
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_update_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Post roll**"
        override fun isEnabled(destinationId: Int) = destinationId == R.id.workspacePrePrint && mode != "off"
        override fun getRightDetail(context: Context) = "10s"
        override suspend fun onClicked(host: MenuHost?) {
            TODO("Not yet implemented")
        }
    }

    class TimelapseArchiveMenuItem : MenuItem {
        override val itemId = "timelapse_archive"
        override var groupId = "archive"
        override val order = 268
        override val showAsSubMenu = true
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_video_library_24

        override fun getTitle(context: Context) = "Timelapse archive**"
        override suspend fun onClicked(host: MenuHost?) {
            TODO("Not yet implemented")
        }
    }
}