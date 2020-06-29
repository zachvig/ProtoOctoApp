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
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectParentViewModel
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_print_controls.*

class PrintControlsFragment : Fragment(R.layout.fragment_print_controls) {

    private val viewModel: PrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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
        })

        widgetsList.adapter = OctoWidgetAdapter().also {
            it.widgets = listOf(
                ProgressWidget(this),
                ControlTemperatureWidget(this)
            )
        }
        buttonTogglePausePrint.setOnClickListener { viewModel.togglePausePrint() }
        buttonMore.setOnClickListener {
            MenuBottomSheet().show(childFragmentManager, "menu")
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
    }

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        private val viewModel: PrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.print_controls_menu

        override fun onMenuItemSelected(id: Int) {
            when (id) {
                R.id.menuChangeFilament -> viewModel.changeFilament()
                R.id.menuOpenOctoprint -> viewModel.getOctoPrintUrl()?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                R.id.menuCancelPrint -> viewModel.cancelPrint()
                R.id.menuEmergencyStop -> viewModel.emergencyStop()
                else -> Unit
            }
        }
    }
}