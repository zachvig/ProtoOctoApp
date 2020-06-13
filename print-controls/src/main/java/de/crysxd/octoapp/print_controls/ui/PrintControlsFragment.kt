package de.crysxd.octoapp.print_controls.ui

import androidx.fragment.app.Fragment
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.R

class PrintControlsFragment : Fragment(R.layout.fragment_print_controls) {

    private val viewModel: PrintControlsViewModel by injectViewModel()

}