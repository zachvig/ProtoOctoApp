package de.crysxd.octoapp.pre_print_controls.ui.extrude

import android.os.Bundle
import android.view.View
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_extrude_filament.*

class ExtrudeControlsFragment : BaseFragment(R.layout.fragment_extrude_filament) {

    override val viewModel: ExtrudeControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buttonExtrude5.setOnClickListener { viewModel.extrude5mm() }
        buttonExtrude25.setOnClickListener { viewModel.extrude25mm() }
        buttonExtrude50.setOnClickListener { viewModel.extrude50mm() }
        buttonExtrudeOther.setOnClickListener { viewModel.extrudeOther(requireContext()) }
    }
}