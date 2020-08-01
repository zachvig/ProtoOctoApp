package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*
import kotlinx.coroutines.delay
import de.crysxd.octoapp.base.R as BaseR

class PrePrintControlsFragment : BaseFragment(R.layout.fragment_pre_print_controls) {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }

        buttonMore.setOnClickListener {
            MenuBottomSheet().show(childFragmentManager)
        }

        lifecycleScope.launchWhenCreated {
            delay(350)
            widgetList.adapter = OctoWidgetAdapter().also {
                it.setWidgets(
                    listOf(
                        ControlTemperatureWidget(this@PrePrintControlsFragment),
                        MoveToolWidget(this@PrePrintControlsFragment),
                        WebcamWidget(this@PrePrintControlsFragment),
                        ExtrudeWidget(this@PrePrintControlsFragment),
                        SendGcodeWidget(this@PrePrintControlsFragment)
                    )
                )
            }
        }

        (widgetList.layoutManager as? StaggeredGridLayoutManager)?.spanCount = resources.getInteger(BaseR.integer.widget_list_span_count)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        widgetListScroller.setupWithToolbar(requireOctoActivity(), bottomAction)
    }

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        private val viewModel: PrePrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.prepare_menu

        override fun onStart() {
            super.onStart()
            val psuSupported = Injector.get().octorPrintRepository().instanceInformation.value?.supportsPsuPlugin == true
            setMenuItemVisibility(R.id.menuItemTurnOffPsu, psuSupported)
        }

        override suspend fun onMenuItemSelected(id: Int): Boolean {
            when (id) {
                R.id.menuChangeFilament -> viewModel.changeFilament()
                R.id.menuItemTurnOffPsu -> viewModel.turnOffPsu()
                else -> return false
            }

            return true
        }
    }
}