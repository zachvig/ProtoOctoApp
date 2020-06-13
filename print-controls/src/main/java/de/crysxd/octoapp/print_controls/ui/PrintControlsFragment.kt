package de.crysxd.octoapp.print_controls.ui

import androidx.fragment.app.Fragment
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel

class PrintControlsFragment : Fragment(R.layout.fragment_print_controls) {

    private val viewModel: PrintControlsViewModel by injectViewModel()

}