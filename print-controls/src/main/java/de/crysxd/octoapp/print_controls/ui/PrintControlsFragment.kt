package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_print_controls.*

class PrintControlsFragment : Fragment(R.layout.fragment_print_controls) {

    private val viewModel: PrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonCancelPrint.setOnClickListener { viewModel.cancelPrint() }
        buttonCancelPrint.setOnLongClickListener { viewModel.emergencyStop(); true }
        buttonTogglePausePrint.setOnClickListener { viewModel.togglePausePrint() }

        viewModel.printState.observe(viewLifecycleOwner, Observer {
            buttonTogglePausePrint.isEnabled = true
            buttonTogglePausePrint.text = if (it.state?.flags?.paused == true) {
                "Resume"
            } else if (it.state?.flags?.pausing == true) {
                buttonTogglePausePrint.isEnabled = false
                "Pausing..."
            } else {
                "Pause"
            }

            buttonCancelPrint.isEnabled = true
            buttonCancelPrint.text = if (it.state?.flags?.cancelling == true) {
                buttonTogglePausePrint.isEnabled = false
                "Cancelling..."
            } else {
                "Cancel"
            }

            textView.text = """
Status: ${formatStatus(it.state)}
Print time: ${it.progress?.printTime?.let(::formatDuration)}
Print time left: ${it.progress?.printTimeLeft?.let(::formatDuration)}
Estimation method: ${it.progress?.printTimeLeftOrigin}"""
        })
    }

    private fun formatDuration(seconds: Int): String = if (seconds < 60) {
        seconds.toString()
    } else {
        DateUtils.formatElapsedTime(seconds.toLong())
    }

    private fun formatStatus(state: PrinterState.State?) = when {
        state?.flags?.pausing == true -> "Pausing"
        state?.flags?.paused == true -> "Paused"
        state?.flags?.printing == true -> "Printing"
        state?.flags?.operational == true -> "Ready"
        state?.flags?.error == true -> "Error"
        state?.flags?.closedOrError == true -> "Error or closed"
        state?.flags?.cancelling == true -> "Cancelling"
        else -> "Unknown"
    }
}