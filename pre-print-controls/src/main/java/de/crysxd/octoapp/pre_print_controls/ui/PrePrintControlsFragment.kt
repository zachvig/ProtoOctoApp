package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*

class PrePrintControlsFragment : BaseFragment(R.layout.fragment_pre_print_controls) {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()

    private lateinit var lastPrinterState: PrinterState

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.printerState.observe(this, Observer(this::onPrinterStateChanged))
    }

    private fun onPrinterStateChanged(state: PollingLiveData.Result<PrinterState>) {
        if (state is PollingLiveData.Result.Success) state.result.also {
            textInputLayoutBedTemp.editText?.setText(
                it.temperature?.bed?.target?.toString() ?: "0"
            )
            textInputLayoutHotendTemp.editText?.setText(
                it.temperature?.tool0?.target?.toString() ?: "0"
            )
        }
    }
}