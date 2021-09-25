package de.crysxd.octoapp

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.crysxd.baseui.ext.requireOctoActivity

class SplashFragment : Fragment(R.layout.splash_fragment) {

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octo.isVisible = false
    }
}