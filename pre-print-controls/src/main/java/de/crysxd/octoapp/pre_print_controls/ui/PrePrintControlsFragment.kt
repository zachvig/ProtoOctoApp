package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import de.crysxd.octoapp.base.R as BaseR

class PrePrintControlsFragment : BaseFragment(R.layout.fragment_pre_print_controls) {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()
    private val adapter = OctoWidgetAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }

        buttonMore.setOnClickListener {
            MenuBottomSheet().show(childFragmentManager)
        }

        viewModel.instanceInformation.observe(viewLifecycleOwner, Observer(this::installApplicableWidgets))
        (widgetList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(BaseR.integer.widget_list_span_count)
    }

    private fun installApplicableWidgets(instance: OctoPrintInstanceInformationV2?) {
        lifecycleScope.launchWhenCreated {
            if (widgetList.adapter == null) {
                widgetList.adapter = adapter
            }

            val widgets = mutableListOf<OctoWidget>()
            widgets.add(ControlTemperatureWidget(this@PrePrintControlsFragment))
            widgets.add(MoveToolWidget(this@PrePrintControlsFragment))

            if (instance?.isWebcamSupported == true) {
                widgets.add(WebcamWidget(this@PrePrintControlsFragment))
            }

            widgets.add(ExtrudeWidget(this@PrePrintControlsFragment))
            widgets.add(SendGcodeWidget(this@PrePrintControlsFragment))

            Timber.i("Installing widgets: ${widgets.map { it::class.java.simpleName }}")
            adapter.setWidgets(requireContext(), widgets)
        }
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

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        override val viewModel: PrePrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.prepare_menu

        override fun onStart() {
            super.onStart()
            lifecycleScope.launchWhenCreated {
                Injector.get().getPowerDevicesUseCase().execute(GetPowerDevicesUseCase.Params(false)).collect {
                    setMenuItemVisibility(R.id.menuItemTurnOffPsu, it.isNotEmpty())
                }
            }
        }

        override suspend fun onMenuItemSelected(id: Int): Boolean {
            when (id) {
                R.id.menuItemTurnOffPsu -> viewModel.turnOffPsu()
                else -> return false
            }

            return true
        }
    }
}