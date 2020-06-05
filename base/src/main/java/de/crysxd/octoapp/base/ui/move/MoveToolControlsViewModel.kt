package de.crysxd.octoapp.base.ui.move

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

    fun jog(xDistance: Float = 0f, yDistance: Float = 0f, zDistance: Float = 0f) = GlobalScope.launch(coroutineExceptionHandler) {
        octoPrintProvider.octoPrint.value?.let {
            jogPrintHeadUseCase.execute(
                JogPrintHeadUseCase.Param(
                    it,
                    xDistance,
                    yDistance,
                    zDistance
                )
            )
        }
    }
}