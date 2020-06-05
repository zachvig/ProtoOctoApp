package de.crysxd.octoapp.base.ui.move

import androidx.lifecycle.MutableLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MoveToolControlsViewModel(
    private val homePrintHeadUseCase: HomePrintHeadUseCase,
    private val jogPrintHeadUseCase: JogPrintHeadUseCase,
    private val octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    val jogResolutionStepsMm = listOf(0.025f, 0.1f, 1f, 10f, 100f)
    val jogResolution = MutableLiveData<Float>(jogResolutionStepsMm[0])

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
            jogResolution.value?.let {
                jogPrintHeadUseCase.execute(
                    JogPrintHeadUseCase.Param(
                        octoPrint,
                        x.applyToDistance(it),
                        y.applyToDistance(it),
                        z.applyToDistance(it)
                    )
                )
            }
        }
    }

    sealed class Direction(val multiplier: Float) {

        fun applyToDistance(distance: Float) = distance * multiplier

        object None : Direction(0f)
        object Positive : Direction(1f)
        object Negative : Direction(-1f)
    }
}