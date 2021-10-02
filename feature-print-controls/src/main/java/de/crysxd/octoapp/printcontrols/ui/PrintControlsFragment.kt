package de.crysxd.octoapp.printcontrols.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.printcontrols.R
import de.crysxd.octoapp.printcontrols.di.injectViewModel
import timber.log.Timber

class PrintControlsFragment : WidgetHostFragment() {

    override val viewModel: PrintControlsViewModel by injectViewModel()
    override val destinationId = "print"
    private val isKeepScreenOn get() = BaseInjector.get().octoPreferences().isKeepScreenOnDuringPrint
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

        BaseInjector.get().octoPreferences().updatedFlow.asLiveData().observe(viewLifecycleOwner) {
            updateKeepScreenOn()
        }
    }

    override fun doReloadWidgets() {
        val webcamSupported = viewModel.webCamSupported.value == true
        val widgets = mutableListOf(
            WidgetType.AnnouncementWidget,
            WidgetType.ProgressWidget,
            WidgetType.ControlTemperatureWidget,
            WidgetType.WebcamWidget,
            WidgetType.PrintQuickAccessWidget,
            WidgetType.GcodePreviewWidget,
            WidgetType.TuneWidget,
            WidgetType.ExtrudeWidget,
            WidgetType.SendGcodeWidget
        ).also {
            if (!webcamSupported) {
                it.remove(WidgetType.WebcamWidget)
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