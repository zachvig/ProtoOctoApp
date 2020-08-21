package de.crysxd.octoapp.base.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

abstract class BaseFragment(@LayoutRes layout: Int = 0) : Fragment(layout) {

    protected abstract val viewModel: BaseViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.navContoller = findNavController()
        requireOctoActivity().observeErrorEvents(viewModel.errorLiveData)
        requireOctoActivity().observerMessageEvents(viewModel.messages)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = true
        requireOctoActivity().octo.alpha = 1f
    }
}