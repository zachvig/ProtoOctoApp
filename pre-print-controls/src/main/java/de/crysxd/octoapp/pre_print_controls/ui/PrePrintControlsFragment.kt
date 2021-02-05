package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.announcement.AnnouncementWidget
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*
import timber.log.Timber
import de.crysxd.octoapp.base.R as BaseR

class PrePrintControlsFragment : WidgetHostFragment(R.layout.fragment_pre_print_controls) {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()
    private val adapter = OctoWidgetAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }

        buttonMore.setOnClickListener {
            MenuBottomSheetFragment().show(childFragmentManager)
        }

        viewModel.webCamSupported.observe(viewLifecycleOwner, Observer(this::installApplicableWidgets))
        (widgetList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(BaseR.integer.widget_list_span_count)
    }

    private fun installApplicableWidgets(webcamSupported: Boolean) {
        lifecycleScope.launchWhenCreated {
            if (widgetList.adapter == null) {
                widgetList.adapter = adapter
            }

            val widgets = mutableListOf<OctoWidget>()
            widgets.add(AnnouncementWidget(this@PrePrintControlsFragment))
            widgets.add(ControlTemperatureWidget(this@PrePrintControlsFragment))
            widgets.add(MoveToolWidget(this@PrePrintControlsFragment))

            if (webcamSupported) {
                widgets.add(WebcamWidget(this@PrePrintControlsFragment))
            }

            widgets.add(ExtrudeWidget(this@PrePrintControlsFragment))
            widgets.add(SendGcodeWidget(this@PrePrintControlsFragment))

            Timber.i("Installing widgets: ${widgets.map { it::class.java.simpleName }}")
            adapter.setWidgets(requireContext(), widgets.filter { it.isVisible() })
        }
    }

    override fun reloadWidgets() {
        installApplicableWidgets(viewModel.webCamSupported.value == true)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        widgetListScroller.setupWithToolbar(requireOctoActivity(), bottomAction)
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