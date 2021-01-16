package de.crysxd.octoapp.print_controls.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.main.KEY_KEEP_SCREEN_ON
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.widget.gcode.GcodePreviewWidget
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
import de.crysxd.octoapp.print_controls.ui.widget.tune.TuneWidget
import kotlinx.android.synthetic.main.fragment_print_controls.*
import timber.log.Timber

class PrintControlsFragment : BaseFragment(R.layout.fragment_print_controls) {

    override val viewModel: PrintControlsViewModel by injectViewModel()
    private val adapter = OctoWidgetAdapter()
    private val isKeepScreenOn get() = Injector.get().sharedPreferences().getBoolean(KEY_KEEP_SCREEN_ON, false)
    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        updateKeepScreenOn()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        updateKeepScreenOn()

        viewModel.instanceInformation.observe(viewLifecycleOwner, Observer(this::installApplicableWidgets))

        buttonMore.setOnClickListener {
            MenuBottomSheetFragment().show(childFragmentManager)
        }

        (widgetsList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(de.crysxd.octoapp.base.R.integer.widget_list_span_count)
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
        Injector.get().sharedPreferences().registerOnSharedPreferenceChangeListener(preferencesListener)
        updateKeepScreenOn()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        widgetListScroller.setupWithToolbar(requireOctoActivity(), bottomAction)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Injector.get().sharedPreferences().unregisterOnSharedPreferenceChangeListener(preferencesListener)
    }

    private fun doAfterConfirmation(@StringRes message: Int, @StringRes button: Int, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(button) { _, _ -> action() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun installApplicableWidgets(instance: OctoPrintInstanceInformationV2?) {
        lifecycleScope.launchWhenCreated {
            widgetsList.adapter = adapter

            val widgets = mutableListOf<OctoWidget>()
            widgets.add(ProgressWidget(this@PrintControlsFragment))
            widgets.add(ControlTemperatureWidget(this@PrintControlsFragment))

            if (instance?.isWebcamSupported == true) {
                widgets.add(WebcamWidget(this@PrintControlsFragment))
            }

            widgets.add(GcodePreviewWidget(this@PrintControlsFragment))
            widgets.add(TuneWidget(this@PrintControlsFragment))
            Timber.i("Installing widgets: ${widgets.map { it::class.java.simpleName }}")
            adapter.setWidgets(requireContext(), widgets.filter { it.isVisible() })
        }
    }

    fun reloadWidgets() {
        installApplicableWidgets(viewModel.instanceInformation.value)
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