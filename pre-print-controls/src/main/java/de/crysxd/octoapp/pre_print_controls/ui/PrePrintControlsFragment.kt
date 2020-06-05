package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.EnterValueFragmentArgs
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_pre_print_controls.*

class PrePrintControlsFragment : BaseFragment(R.layout.fragment_pre_print_controls) {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setupWithNavController(findNavController())
        toolbar.inflateMenu(R.menu.prepare_menu)
        toolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.menuItemTurnOffPsu -> {
                    viewModel.turnOffPsu()
                    true
                }
                R.id.menuItemSendGcode -> {
                    findNavController().navigate(R.id.action_enter_gcode, EnterValueFragmentArgs(
                        title = getString(R.string.send_gcode),
                        hint = getString(R.string.gcode_one_command_per_line),
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                        maxLines = 10,
                        resultId = viewModel.waitForGcodeCommand()
                    ).toBundle())
                    true
                }
                else -> false
            }
        }

        viewModel.gcodeCommands.observe(this, Observer {
            viewModel.executeGcode(it)
        })
    }
}