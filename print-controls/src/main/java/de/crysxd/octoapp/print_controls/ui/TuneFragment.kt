package de.crysxd.octoapp.print_controls.ui

import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.print_controls.R
import kotlinx.android.synthetic.main.fragment_tune.*

class TuneFragment : Fragment(R.layout.fragment_tune) {

    override fun onStart() {
        super.onStart()

        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        octoScrollView.setupWithToolbar(
            requireOctoActivity(),
            buttonApply
        )
    }
}