package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.announcement.AnnouncementWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.widget.gcode.GcodePreviewWidget
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
import de.crysxd.octoapp.print_controls.ui.widget.tune.TuneWidget
import timber.log.Timber

class PrintControlsFragment : WidgetHostFragment() {

    override val viewModel: PrintControlsViewModel by injectViewModel()
    override val destinationId = "print"
    private val isKeepScreenOn get() = Injector.get().octoPreferences().isKeepScreenOnDuringPrint
    override val toolbarState = OctoToolbar.State.Print

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moreButton.setOnClickListener { MenuBottomSheetFragment().show(childFragmentManager) }
        viewModel.webCamSupported.observe(viewLifecycleOwner) { reloadWidgets() }

        viewModel.printState.observe(viewLifecycleOwner) {
            val isPaused = it.state?.flags?.paused == true
            mainButton.isEnabled = true
            mainButton.setText(
                when {
                    isPaused -> R.string.resume

                    it.state?.flags?.pausing == true -> {
                        mainButton.isEnabled = false
                        R.string.pausing
                    }

                    it.state?.flags?.cancelling == true -> {
                        mainButton.isEnabled = false
                        R.string.cancelling
                    }

                    else -> R.string.pause
                }
            )

            mainButton.setOnClickListener {
                doAfterConfirmation(
                    message = if (isPaused) R.string.resume_print_confirmation_message else R.string.pause_print_confirmation_message,
                    button = if (isPaused) R.string.resume_print_confirmation_action else R.string.pause_print_confirmation_action
                ) {
                    viewModel.togglePausePrint()
                }
            }
        }

        Injector.get().octoPreferences().updatedFlow.asLiveData().observe(viewLifecycleOwner) {
            updateKeepScreenOn()
        }
    }

    override fun reloadWidgets() {
        super.reloadWidgets()
        val webcamSupported = viewModel.webCamSupported.value == true
        val widgets = mutableListOf(
            AnnouncementWidget::class,
            ProgressWidget::class,
            ControlTemperatureWidget::class,
            WebcamWidget::class,
            GcodePreviewWidget::class,
            TuneWidget::class,
        ).also {
            if (!webcamSupported) {
                it.remove(WebcamWidget::class)
            }
        }

        installWidgets(widgets)
    }

    private fun updateKeepScreenOn() {
        if (isKeepScreenOn) {
            Timber.i("Keeping screen on")
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            Timber.i("Not keeping screen on")
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStart() {
        super.onStart()
        updateKeepScreenOn()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun doAfterConfirmation(@StringRes message: Int, @StringRes button: Int, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(button) { _, _ -> action() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}