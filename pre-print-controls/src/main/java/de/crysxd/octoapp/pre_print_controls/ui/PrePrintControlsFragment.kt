package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidgetAdapter
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*

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

        widgetList.adapter = OctoWidgetAdapter().also {
            it.widgets = listOf(ControlTemperatureWidget(this))
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        requireOctoActivity().octo.isVisible = true
        widgetList.setupWithToolbar(requireOctoActivity())
    }

    class MenuBottomSheet : de.crysxd.octoapp.base.ui.common.MenuBottomSheet() {

        private val viewModel: PrePrintControlsViewModel by injectParentViewModel()

        override fun getMenuRes() = R.menu.prepare_menu

        override fun onMenuItemSelected(id: Int) {
            when (id) {
                R.id.menuItemTurnOffPsu -> viewModel.turnOffPsu()
                R.id.menuItemSendGcode -> viewModel.executeGcode(requireContext())
                else -> Unit
            }
        }
    }
}