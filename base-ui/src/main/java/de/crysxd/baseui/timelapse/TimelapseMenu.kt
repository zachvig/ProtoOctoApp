package de.crysxd.baseui.timelapse

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import de.crysxd.baseui.R
import de.crysxd.baseui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.RevolvingOptionsMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseConfig
import kotlinx.coroutines.flow.collect
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat

@Parcelize
class TimelapseMenu : Menu {

    override suspend fun getTitle(context: Context) = "Timelapse Config**"

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
            TimelapseIntervalItem(config),
            PersistTimelapseConfigMenuItem(),
        )
    }

    companion object {
        private suspend fun MenuItem.askForValue(
            host: MenuHost?,
            value: String,
            inputType: Int = InputType.TYPE_CLASS_NUMBER,
            update: (String?, TimelapseConfig) -> TimelapseConfig,
        ) {
            val result = NavigationResultMediator.registerResultCallback<String?>()
            val context = host?.requireContext() ?: return
            val navController = host.getNavController() ?: return
            navController.navigate(
                R.id.action_enter_value,
                EnterValueFragmentArgs(
                    title = getTitle(context).toString(),
                    action = "Update**",
                    resultId = result.first,
                    hint = getTitle(context).toString(),
                    value = value,
                    inputType = inputType,
                    selectAll = true
                ).toBundle()
            )

            result.second.asFlow().collect {
                BaseInjector.get().timelapseRepository().update {
                    update(it, this)
                }
                host.reloadMenu()
            }
        }
    }

    class TimelapseModeMenuItem(private val config: TimelapseConfig) : RevolvingOptionsMenuItem() {
        override val itemId = "timelapse_mode"
        override var groupId = ""
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
            BaseInjector.get().timelapseRepository().update { copy(type = TimelapseConfig.Type.valueOf(option.value)) }
            host?.reloadMenu()
        }
    }

    class TimelapseMinimumIntervalMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_minimum_interval"
        override var groupId = ""
        override val order = 263
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_access_time_24

        override fun getTitle(context: Context) = "Minimum interval**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint && config.type == TimelapseConfig.Type.ZChange
        override fun getRightDetail(context: Context) = config.minDelay?.let { context.getString(R.string.x_secs, it) }
        override suspend fun onClicked(host: MenuHost?) {
            askForValue(host = host, value = config.minDelay?.toString() ?: "") { newValue, config ->
                config.copy(minDelay = newValue?.toIntOrNull() ?: 0)
            }
        }
    }

    class TimelapseIntervalItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_interval"
        override var groupId = ""
        override val order = 263
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_access_time_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Interval**"
        override fun isVisible(destinationId: Int) = config.type == TimelapseConfig.Type.Timed && destinationId == R.id.workspacePrePrint
        override fun getRightDetail(context: Context) = config.interval?.let { context.getString(R.string.x_secs, it) }
        override suspend fun onClicked(host: MenuHost?) {
            askForValue(host = host, value = config.interval?.toString() ?: "") { newValue, config ->
                config.copy(interval = newValue?.toIntOrNull() ?: 0)
            }
        }
    }

    class TimelapseZHopMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_z_hop"
        override var groupId = ""
        override val order = 264
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_height_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Retraction Z-Hop**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint && config.type == TimelapseConfig.Type.ZChange
        override fun getRightDetail(context: Context) = config.retractionZHop?.let { context.getString(R.string.x_mm, NumberFormat.getInstance().format(it)) }
        override suspend fun onClicked(host: MenuHost?) {
            askForValue(
                host = host,
                value = config.minDelay?.toString() ?: "",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ) { newValue, config ->
                config.copy(retractionZHop = NumberFormat.getInstance().parse(newValue ?: "0")?.toFloat() ?: 0f)
            }
        }
    }

    class TimelapseFrameRateMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_frame_rate"
        override var groupId = ""
        override val order = 265
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_burst_mode_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Frame rate**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun isEnabled(destinationId: Int) = config.type != TimelapseConfig.Type.Off
        override fun getRightDetail(context: Context) = config.fps?.let { context.getString(R.string.x_fps, it) }
        override suspend fun onClicked(host: MenuHost?) {
            askForValue(host = host, value = config.fps?.toString() ?: "") { newValue, config ->
                config.copy(fps = newValue?.toIntOrNull() ?: 0)
            }
        }
    }

    class TimelapsePostRollMenuItem(private val config: TimelapseConfig) : MenuItem {
        override val itemId = "timelapse_post_roll"
        override var groupId = ""
        override val order = 266
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_update_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Post roll**"
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun isEnabled(destinationId: Int) = config.type != TimelapseConfig.Type.Off
        override fun getRightDetail(context: Context) = config.postRoll?.let { context.getString(R.string.x_secs, it) }
        override suspend fun onClicked(host: MenuHost?) {
            askForValue(host = host, value = config.postRoll?.toString() ?: "") { newValue, config ->
                config.copy(postRoll = newValue?.toIntOrNull() ?: 0)
            }
        }
    }

    class AskForTimelapseBeforePrintingMenuItem : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().askForTimelapseBeforePrinting
        override val itemId = "timelapse_ask_before_printing"
        override var groupId = "settings"
        override val order = 270
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_control_camera_24
        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override fun getTitle(context: Context) = "Ask before printing**"
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().askForTimelapseBeforePrinting = enabled
        }
    }

    class PersistTimelapseConfigMenuItem : MenuItem {
        override val itemId = "timelapse_persist"
        override var groupId = "settings"
        override val order = 271
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_save_24
        override val canBePinned = false

        override fun getTitle(context: Context) = "Save as default**"
        override fun getDescription(context: Context) =
            "The timelapse config is reset when OctoPrint restarts. This option allows you to set the current config as default across restarts**"

        override fun isVisible(destinationId: Int) = destinationId == R.id.workspacePrePrint
        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(save = true)
            }
        }
    }
}