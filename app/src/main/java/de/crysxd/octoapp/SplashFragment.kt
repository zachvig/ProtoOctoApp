package de.crysxd.octoapp

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octo.isVisible = false
    }
}