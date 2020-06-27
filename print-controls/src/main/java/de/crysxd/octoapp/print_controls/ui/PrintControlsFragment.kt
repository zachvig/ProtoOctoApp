package de.crysxd.octoapp.print_controls.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.ui.common.MenuBottomSheet
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectParentViewModel
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_print_controls.*

class PrintControlsFragment : Fragment(R.layout.fragment_print_controls) {

    private val viewModel: PrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonTogglePausePrint.setOnClickListener { viewModel.togglePausePrint() }

        viewModel.printState.observe(viewLifecycleOwner, Observer {
            buttonTogglePausePrint.isEnabled = true
            buttonTogglePausePrint.text = if (it.state?.flags?.paused == true) {
                "Resume"
            } else if (it.state?.flags?.pausing == true) {
                buttonTogglePausePrint.isEnabled = false
                "Pausing..."
            } else if (it.state?.flags?.cancelling == true) {
                buttonTogglePausePrint.isEnabled = false
                "Cancelling..."
            } else {
                "Pause"
            }

            textView.text = """
Status: ${formatStatus(it.state)}
Print time: ${it.progress?.printTime?.let(::formatDuration)}
Print time left: ${it.progress?.printTimeLeft?.let(::formatDuration)}
Estimation method: ${it.progress?.printTimeLeftOrigin}"""
        })

        buttonMore.setOnClickListener {
            MenuBottomSheet().show(childFragmentManager, "menu")
        }
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

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        private val viewModel: PrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.print_controls_menu

        override fun onMenuItemSelected(id: Int) {
            when (id) {
                R.id.menuChangeFilament -> Unit
                R.id.menuOpenOctoprint -> viewModel.getOctoPrintUrl()?.let {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
                R.id.menuCancelPrint -> viewModel.cancelPrint()
                R.id.menuEmergencyStop -> viewModel.emergencyStop()
                else -> Unit
            }
        }
    }
}