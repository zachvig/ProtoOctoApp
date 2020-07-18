package de.crysxd.octoapp.pre_print_controls.ui.widget.move

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
        octoPrintProvider.octoPrint.value?.let {
            homePrintHeadUseCase.execute(Pair(it, HomePrintHeadUseCase.Axis.XY))
        }
    }

    fun homeZAxis() = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            homePrintHeadUseCase.execute(Pair(it, HomePrintHeadUseCase.Axis.Z))
        }
    }

    fun jog(x: Direction = Direction.None, y: Direction = Direction.None, z: Direction = Direction.None) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let { octoPrint ->
            jogPrintHeadUseCase.execute(
                JogPrintHeadUseCase.Param(
                    octoPrint,
                    x.applyToDistance(jogResolution),
                    y.applyToDistance(jogResolution),
                    z.applyToDistance(jogResolution)
                )
            )
        }
    }

    sealed class Direction(val multiplier: Float) {

        fun applyToDistance(distance: Float) = distance * multiplier

        object None : Direction(0f)
        object Positive : Direction(1f)
        object Negative : Direction(-1f)
    }
}