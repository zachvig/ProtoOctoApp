package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*

class PrePrintControlsFragment : BaseFragment(R.layout.fragment_pre_print_controls) {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Manually disable the back navigation, we can't go back
       /* toolbar.setupWithNavController(findNavController())
        toolbar.navigationIcon = null
        toolbar.setNavigationOnClickListener {  }

        toolbar.inflateMenu(R.menu.prepare_menu)
        toolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.menuItemTurnOffPsu -> {
                    viewModel.turnOffPsu()
                    true
                }
                R.id.menuItemSendGcode -> {
                    viewModel.executeGcode(requireContext())
                    true
                }
                else -> false
            }
        }*/

        scrollView.setupWithToolbar(requireOctoActivity().octoToolbar)

        buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
    }
}