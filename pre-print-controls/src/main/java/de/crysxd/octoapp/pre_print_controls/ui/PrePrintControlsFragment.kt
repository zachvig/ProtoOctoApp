package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.announcement.AnnouncementWidget
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.pre_print_controls.databinding.PrePrintControlsFragmentBinding
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget

class PrePrintControlsFragment : WidgetHostFragment() {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()
    private lateinit var binding: PrePrintControlsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        PrePrintControlsFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }

        binding.buttonMore.setOnClickListener {
            MenuBottomSheetFragment().show(childFragmentManager)
        }

        binding.widgetList.connectToLifecycle(viewLifecycleOwner)

        viewModel.webCamSupported.observe(viewLifecycleOwner, Observer(this::installApplicableWidgets))
        // TODO add columns
        //  (widgetList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(BaseR.integer.widget_list_span_count)
    }


    private fun installApplicableWidgets(webcamSupported: Boolean) {
        binding.widgetList.showWidgets(
            parent = this,
            widgetClasses = listOf(
                AnnouncementWidget::class,
                ControlTemperatureWidget::class,
                MoveToolWidget::class,
                WebcamWidget::class,
                SendGcodeWidget::class,
                ExtrudeWidget::class,
            )
        )
    }

    override fun requestTransition() {
        TransitionManager.beginDelayedTransition(binding.root)
    }

    override fun reloadWidgets() {
        installApplicableWidgets(viewModel.webCamSupported.value == true)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        binding.widgetListScroller.setupWithToolbar(requireOctoActivity(), binding.bottomAction)
    }

    override fun onResume() {
        super.onResume()
        // TODO
        // adapter.dispatchResume()
    }

    override fun onPause() {
        super.onPause()
        // TODO
        // adapter.dispatchPause()
    }
}