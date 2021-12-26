package de.crysxd.octoapp.printcontrols.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
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
        bottomToolbar.menuButton.setOnClickListener { MenuBottomSheetFragment().show(childFragmentManager) }
        viewModel.webCamSupported.observe(viewLifecycleOwner) { reloadWidgets("webcam-suppport-change") }

        val cancelAction = bottomToolbar.addAction(
            icon = R.drawable.ic_round_stop_24,
            title = R.string.cancel_print,
            needsSwipe = true,
            action = viewModel::cancelPrint
        )
        val pauseAction = bottomToolbar.addAction(
            icon = R.drawable.ic_round_pause_24,
            title = R.string.pause,
            needsSwipe = true,
            action = viewModel::togglePausePrint
        )
        val resumeAction = bottomToolbar.addAction(
            icon = R.drawable.ic_round_play_arrow_24,
            title = R.string.resume,
            needsSwipe = true,
            action = viewModel::togglePausePrint
        )


        var lastStatus: String? = "initial"
        viewModel.printState.observe(viewLifecycleOwner) { msg ->
            val printing = msg.state?.flags?.printing == true
            val paused = msg.state?.flags?.paused == true
            val pausing = msg.state?.flags?.pausing == true
            val cancelling = msg.state?.flags?.cancelling == true
            val status = when {
                pausing -> getString(R.string.pausing)
                cancelling -> getString(R.string.cancelling)
                paused -> getString(R.string.paused)
                printing && msg.progress?.completion != null -> getString(R.string.x_percent_int, msg.progress?.completion?.toInt())
                else -> null
            }

            val cancelActionVisible = printing || paused
            val pauseActionVisible = printing && !pausing
            val resumeActionVisible = paused && !cancelling

            if (
                lastStatus != status ||
                cancelActionVisible != cancelAction.isVisible ||
                pauseActionVisible != pauseAction.isVisible ||
                resumeActionVisible != resumeAction.isVisible
            ) {
                bottomToolbar.startDelayedTransition()
                bottomToolbar.setStatus(status)
                lastStatus = status
                cancelAction.isVisible = cancelActionVisible
                pauseAction.isVisible = pauseActionVisible
                resumeAction.isVisible = resumeActionVisible
            }
        }

        BaseInjector.get().octoPreferences().updatedFlow.asLiveData().observe(viewLifecycleOwner) {
            updateKeepScreenOn()
        }
    }

    private var List<View>.isVisible: Boolean
        get() = all { it.isVisible }
        set(value) = forEach { it.isVisible = value }

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
}