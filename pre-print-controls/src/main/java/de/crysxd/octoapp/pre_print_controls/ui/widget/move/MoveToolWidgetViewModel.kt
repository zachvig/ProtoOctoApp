package de.crysxd.octoapp.pre_print_controls.ui.widget.move

import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val DEFAULT_FEED_RATE = 4000
const val KEY_Z_FEED_RATE = "move_tool_z_feed_rate"

class MoveToolWidgetViewModel(
    private val homePrintHeadUseCase: HomePrintHeadUseCase,
    private val jogPrintHeadUseCase: JogPrintHeadUseCase,
    private val sharedPreferences: SharedPreferences
) : BaseViewModel() {

    var jogResolution: Float = -1f

    fun homeXYAxis() = GlobalScope.launch(coroutineExceptionHandler) {
        homePrintHeadUseCase.execute(HomePrintHeadUseCase.Axis.XY)
    }

    fun homeZAxis() = GlobalScope.launch(coroutineExceptionHandler) {
        homePrintHeadUseCase.execute(HomePrintHeadUseCase.Axis.Z)
    }

    fun jog(x: Direction = Direction.None, y: Direction = Direction.None, z: Direction = Direction.None) = GlobalScope.launch(coroutineExceptionHandler) {
        jogPrintHeadUseCase.execute(
            JogPrintHeadUseCase.Param(
                x.applyToDistance(jogResolution),
                y.applyToDistance(jogResolution),
                z.applyToDistance(jogResolution),
                getFeedRate(x, y, z)
            )
        )
    }

    fun showSettings(context: Context) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.move_settings),
                hint = context.getString(R.string.z_feedrate_mm_min),
                action = context.getString(R.string.update_settings),
                resultId = result.first,
                value = getZFeedRate().toString(),
                inputType = InputType.TYPE_CLASS_NUMBER,
                selectAll = true
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            result.second.asFlow().first()
        }?.let { feedRate ->
            setZFeedRate(feedRate.toIntOrNull() ?: DEFAULT_FEED_RATE)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getFeedRate(x: Direction = Direction.None, y: Direction = Direction.None, z: Direction = Direction.None) = when {
        z != Direction.None -> getZFeedRate()
        else -> DEFAULT_FEED_RATE
    }

    private fun getZFeedRate() = sharedPreferences.getInt(KEY_Z_FEED_RATE, DEFAULT_FEED_RATE)

    private fun setZFeedRate(feedRate: Int) = sharedPreferences.edit {
        putInt(KEY_Z_FEED_RATE, feedRate)
    }

    sealed class Direction(val multiplier: Float) {

        fun applyToDistance(distance: Float) = distance * multiplier

        object None : Direction(0f)
        object Positive : Direction(1f)
        object Negative : Direction(-1f)
    }
}