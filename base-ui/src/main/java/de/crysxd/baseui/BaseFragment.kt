package de.crysxd.baseui

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.requireOctoActivity

abstract class BaseFragment(@LayoutRes layout: Int = 0) : Fragment(layout) {

    protected abstract val viewModel: BaseViewModel

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.navContoller = findNavController()
        requireOctoActivity().observeErrorEvents(viewModel.errorLiveData)
        requireOctoActivity().observerMessageEvents(viewModel.messages)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = true
        requireOctoActivity().octo.alpha = 1f
    }
}