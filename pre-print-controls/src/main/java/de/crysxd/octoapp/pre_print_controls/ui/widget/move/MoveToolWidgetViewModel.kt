package de.crysxd.octoapp.pre_print_controls.ui.widget.move

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MoveToolWidgetViewModel(
    private val homePrintHeadUseCase: HomePrintHeadUseCase,
    private val jogPrintHeadUseCase: JogPrintHeadUseCase,
    private val octoPrintProvider: OctoPrintProvider
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
                z.applyToDistance(jogResolution)
            )
        )
    }

    fun showSettings() = viewModelScope.launch(coroutineExceptionHandler) {

    }

    sealed class Direction(val multiplier: Float) {

        fun applyToDistance(distance: Float) = distance * multiplier

        object None : Direction(0f)
        object Positive : Direction(1f)
        object Negative : Direction(-1f)
    }
}