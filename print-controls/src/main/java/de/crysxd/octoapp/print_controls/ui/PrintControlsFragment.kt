package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectParentViewModel
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
import kotlinx.android.synthetic.main.fragment_print_controls.*

class PrintControlsFragment : BaseFragment(R.layout.fragment_print_controls) {

    override val viewModel: PrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel.printState.observe(viewLifecycleOwner, Observer {
            val isPaused = it.state?.flags?.paused == true
            buttonTogglePausePrint.isEnabled = true
            buttonTogglePausePrint.setText(
                when {
                    isPaused -> R.string.resume

                    it.state?.flags?.pausing == true -> {
                        buttonTogglePausePrint.isEnabled = false
                        R.string.pausing
                    }

                    it.state?.flags?.cancelling == true -> {
                        buttonTogglePausePrint.isEnabled = false
                        R.string.cancelling
                    }

                    else -> R.string.pause
                }
            )

            buttonTogglePausePrint.setOnClickListener {
                doAfterConfirmation(
                    message = if (isPaused) R.string.resume_print_confirmation_message else R.string.pause_print_confirmation_message,
                    button = if (isPaused) R.string.resume_print_confirmation_action else R.string.pause_print_confirmation_action
                ) {
                    viewModel.togglePausePrint()
                }
            }
        })

        widgetsList.adapter = OctoWidgetAdapter().also {
            it.widgets = listOf(
                ProgressWidget(this),
                ControlTemperatureWidget(this),
                WebcamWidget(this)
            )
        }

        buttonMore.setOnClickListener {
            MenuBottomSheet().show(childFragmentManager, "menu")
        }

        (widgetsList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(de.crysxd.octoapp.base.R.integer.widget_list_span_count)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        widgetsList.setupWithToolbar(requireOctoActivity())
    }

    private fun doAfterConfirmation(@StringRes message: Int, @StringRes button: Int, action: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(button) { _, _ -> action() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        private val viewModel: PrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.print_controls_menu

        override suspend fun onMenuItemSelected(id: Int): Boolean {
            // Get ViewModel, will be used after confirmation dialog
            viewModel

            when (id) {
                R.id.menuChangeFilament -> doAfterConfirmation(
                    message = R.string.change_filament_confirmation_message,
                    button = R.string.change_filament_confirmation_action
                ) {
                    viewModel.changeFilament()
                }
                R.id.menuCancelPrint -> doAfterConfirmation(
                    message = R.string.cancel_print_confirmation_message,
                    button = R.string.cancel_print_confirmation_action
                ) {
                    viewModel.cancelPrint()
                }
                R.id.menuEmergencyStop -> doAfterConfirmation(
                    message = R.string.emergency_stop_confirmation_message,
                    button = R.string.emergency_stop_confirmation_action
                ) {
                    viewModel.emergencyStop()
                }
                else -> return false
            }

            return true
        }

        private fun doAfterConfirmation(@StringRes message: Int, @StringRes button: Int, action: () -> Unit) {
            AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton(button) { _, _ -> action() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}