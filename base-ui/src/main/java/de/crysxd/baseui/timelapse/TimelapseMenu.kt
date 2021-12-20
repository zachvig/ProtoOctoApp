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
class TimelapseMenu(private val startPrintResultId: Int? = null) : Menu {

    override suspend fun getTitle(context: Context) = context.getString(R.string.timelapse_config___title)

    override suspend fun getMenuItem(): List<MenuItem> {
        val config = BaseInjector.get().timelapseRepository().fetchLatest().config
        requireNotNull(config)
        return listOfNotNull(

            TimelapseModeMenuItem(BaseInjector.get().localizedContext(), config),
            TimelapseMinimumIntervalMenuItem(config),
            TimelapseZHopMenuItem(config),
            TimelapseFrameRateMenuItem(config),
            TimelapsePostRollMenuItem(config),
            TimelapseIntervalItem(config),

            AskForTimelapseBeforePrintingMenuItem(),
            PersistTimelapseConfigMenuItem(),
            startPrintResultId?.let { StartPrintMenuItem(startPrintResultId) },
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // If we already sent a result (by clicking start print) this won't do anything as only one result can be send
        startPrintResultId?.let {
            NavigationResultMediator.postResult(it, false)
        }
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
                    action = context.getString(R.string.timelapse_config___change),
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

    class TimelapseModeMenuItem(context: Context, private val config: TimelapseConfig) : RevolvingOptionsMenuItem() {
        override val itemId = "timelapse_mode"
        override var groupId = ""
        override val order = 262
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_videocam_24
        override val activeValue get() = (config.type ?: TimelapseConfig.Type.Off).name
        override val options = listOf(
            Option(label = context.getString(R.string.timelapse_config___mode___off), value = TimelapseConfig.Type.Off.name),
            Option(label = context.getString(R.string.timelapse_config___mode___timed), value = TimelapseConfig.Type.Timed.name),
            Option(label = context.getString(R.string.timelapse_config___mode___zchange), value = TimelapseConfig.Type.ZChange.name),
        )

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___mode)
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

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___min_interval)
        override fun isVisible(destinationId: Int) = config.type == TimelapseConfig.Type.ZChange
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

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___interval)
        override fun isVisible(destinationId: Int) = config.type == TimelapseConfig.Type.Timed
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

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___retraction_z_hop)
        override fun isVisible(destinationId: Int) = config.type == TimelapseConfig.Type.ZChange
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

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___frame_rate)
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

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___post_roll)
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
        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___ask_before_printing)
        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().askForTimelapseBeforePrinting = enabled
        }
    }

    class PersistTimelapseConfigMenuItem() : MenuItem {
        override val itemId = "timelapse_persist"
        override var groupId = "settings"
        override val order = 271
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_save_24
        override val canBePinned = false

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___save_as_default)
        override fun getDescription(context: Context) = context.getString(R.string.timelapse_config___save_as_default___description)

        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().update {
                copy(save = true)
            }
        }
    }

    class StartPrintMenuItem(private val resultId: Int) : MenuItem {
        override val itemId = "timelapse_start_print"
        override var groupId = "print"
        override val order = 272
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_send_24
        override val canBePinned = false

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_config___start_print)

        override suspend fun onClicked(host: MenuHost?) {
            NavigationResultMediator.postResult(resultId, true)
        }
    }
}