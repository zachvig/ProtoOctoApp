package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.announcement.AnnouncementWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.databinding.FragmentPrintControlsBinding
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.widget.gcode.GcodePreviewWidget
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
import kotlinx.android.synthetic.main.fragment_print_controls.*
import timber.log.Timber

class PrintControlsFragment : WidgetHostFragment() {

    override val viewModel: PrintControlsViewModel by injectViewModel()
    private val adapter = OctoWidgetAdapter()
    private val isKeepScreenOn get() = Injector.get().octoPreferences().isKeepScreenOnDuringPrint
    private lateinit var binding: FragmentPrintControlsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentPrintControlsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.widgetList.connectToLifecycle(viewLifecycleOwner)
        viewModel.printState.observe(viewLifecycleOwner) {
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
        }

        Injector.get().octoPreferences().updatedFlow.asLiveData().observe(viewLifecycleOwner) {
            updateKeepScreenOn()
        }

        viewModel.webCamSupported.observe(viewLifecycleOwner, Observer(this::installApplicableWidgets))

        buttonMore.setOnClickListener {
            MenuBottomSheetFragment().show(childFragmentManager)
        }
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
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        widgetListScroller.setupWithToolbar(requireOctoActivity(), bottomAction)
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

    private fun installApplicableWidgets(webcamSupported: Boolean) {
        binding.widgetList.showWidgets(
            parent = this,
            widgetClasses = mutableListOf(
                AnnouncementWidget::class,
                ProgressWidget::class,
                ControlTemperatureWidget::class,
                WebcamWidget::class,
                GcodePreviewWidget::class,
            ).also {
                if (!webcamSupported) {
                    it.remove(WebcamWidget::class)
                }
            }
        )
    }

    override fun reloadWidgets() {
        Timber.i("Reload widgets")
        installApplicableWidgets(viewModel.webCamSupported.value == true)
    }

    override fun onResume() {
        super.onResume()
        adapter.dispatchResume()
    }

    override fun onPause() {
        super.onPause()
        adapter.dispatchPause()
    }
}