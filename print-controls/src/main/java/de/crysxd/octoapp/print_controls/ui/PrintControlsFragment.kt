package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectParentViewModel
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
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

        (widgetsList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(de.crysxd.octoapp.base.R.integer.widget_list_span_count)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        requireOctoActivity().octo.isVisible = true
    }

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        private val viewModel: PrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.print_controls_menu

        override suspend fun onMenuItemSelected(id: Int): Boolean {
            when (id) {
                R.id.menuChangeFilament -> viewModel.changeFilament()
                R.id.menuCancelPrint -> viewModel.cancelPrint()
                R.id.menuEmergencyStop -> viewModel.emergencyStop()
                else -> return false
            }

            return true
        }
    }
}