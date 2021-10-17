package de.crysxd.octoapp.preprintcontrols.ui.widget.move

import android.content.Context
import android.text.InputType
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.preprintcontrols.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MoveToolWidgetViewModel(
    private val homePrintHeadUseCase: HomePrintHeadUseCase,
    private val jogPrintHeadUseCase: JogPrintHeadUseCase,
    private val octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    companion object {
        private const val DEFAULT_FEED_RATE = 4000
    }

    var jogResolution: Float = -1f

    fun homeXYAxis() = AppScope.launch(coroutineExceptionHandler) {
        homePrintHeadUseCase.execute(HomePrintHeadUseCase.Axis.XY)
    }

    fun homeZAxis() = AppScope.launch(coroutineExceptionHandler) {
        homePrintHeadUseCase.execute(HomePrintHeadUseCase.Axis.Z)
    }

    fun jog(x: Direction = Direction.None, y: Direction = Direction.None, z: Direction = Direction.None) = AppScope.launch(coroutineExceptionHandler) {
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
        val resultX = NavigationResultMediator.registerResultCallback<String?>()
        val resultY = NavigationResultMediator.registerResultCallback<String?>()
        val resultZ = NavigationResultMediator.registerResultCallback<String?>()

        navContoller.navigate(
            R.id.action_enter_value,
            EnterValueFragmentArgs(
                title = context.getString(R.string.move_settings),
                action = context.getString(R.string.update_settings),
                selectAll = true,

                hint = context.getString(R.string.x_feedrate_mm_min),
                value = getXFeedRate().toString(),
                inputType = InputType.TYPE_CLASS_NUMBER,
                resultId = resultX.first,

                hint2 = context.getString(R.string.y_feedrate_mm_min),
                value2 = getYFeedRate().toString(),
                inputType2 = InputType.TYPE_CLASS_NUMBER,
                resultId2 = resultY.first,

                hint3 = context.getString(R.string.z_feedrate_mm_min),
                value3 = getZFeedRate().toString(),
                inputType3 = InputType.TYPE_CLASS_NUMBER,
                resultId3 = resultZ.first,
            ).toBundle()
        )

        withContext(Dispatchers.Default) {
            suspend fun Pair<Int, LiveData<String?>>.readFeedRate() = second.asFlow().first()?.toIntOrNull() ?: DEFAULT_FEED_RATE
            setFeedRates(xFeedRate = resultX.readFeedRate(), yFeedRate = resultY.readFeedRate(), zFeedRate = resultZ.readFeedRate())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getFeedRate(x: Direction = Direction.None, y: Direction = Direction.None, z: Direction = Direction.None) = when {
        x != Direction.None -> getXFeedRate()
        y != Direction.None -> getYFeedRate()
        z != Direction.None -> getZFeedRate()
        else -> DEFAULT_FEED_RATE
    }

    private fun getXFeedRate() = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.moveXFeedRate ?: DEFAULT_FEED_RATE
    private fun getYFeedRate() = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.moveYFeedRate ?: DEFAULT_FEED_RATE
    private fun getZFeedRate() = octoPrintRepository.getActiveInstanceSnapshot()?.appSettings?.moveZFeedRate ?: DEFAULT_FEED_RATE

    private fun setFeedRates(xFeedRate: Int, yFeedRate: Int, zFeedRate: Int) = viewModelScope.launch(coroutineExceptionHandler) {
        Timber.i("xFeedRate=$xFeedRate yFeedRate=$yFeedRate zFeedRate=$zFeedRate")
        octoPrintRepository.updateAppSettingsForActive {
            it.copy(moveZFeedRate = zFeedRate, moveXFeedRate = xFeedRate, moveYFeedRate = yFeedRate)
        }
    }

    sealed class Direction(val multiplier: Float) {

        fun applyToDistance(distance: Float) = distance * multiplier

        object None : Direction(0f)
        object Positive : Direction(1f)
        object Negative : Direction(-1f)
    }
}