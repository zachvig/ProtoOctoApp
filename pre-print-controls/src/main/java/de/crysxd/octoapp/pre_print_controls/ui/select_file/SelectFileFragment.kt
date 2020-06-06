package de.crysxd.octoapp.pre_print_controls.ui.select_file

import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel

class SelectFileFragment : BaseFragment(R.layout.fragment_select_file) {

    override val viewModel: SelectFileViewModel by injectViewModel()

}