package de.crysxd.octoapp.base.ui.move

import android.os.Bundle
import android.view.View
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_move_tool_controls.*

class MoveToolControlsFragment : BaseFragment(R.layout.fragment_move_tool_controls) {

    override val viewModel: MoveToolControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setOnClickListener {
            viewModel.homeZAxis()
        }

        homeXY.setOnClickListener {
            viewModel.homeXYAxis()
        }
    }
}