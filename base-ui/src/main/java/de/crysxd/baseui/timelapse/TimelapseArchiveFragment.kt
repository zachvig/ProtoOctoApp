package de.crysxd.baseui.timelapse

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.databinding.TimelapseArchiveFragmentBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.timelapse.TimelapseArchiveViewModel.ViewState.Error
import de.crysxd.baseui.timelapse.TimelapseArchiveViewModel.ViewState.Idle
import de.crysxd.baseui.timelapse.TimelapseArchiveViewModel.ViewState.Loading
import de.crysxd.baseui.utils.CollapsibleToolbarTabsHelper
import de.crysxd.octoapp.base.ext.composeErrorMessage
import de.crysxd.octoapp.base.ext.toHtml

class TimelapseArchiveFragment : BaseFragment(), InsetAwareScreen {
    override val viewModel by injectViewModel<TimelapseArchiveViewModel>()
    private lateinit var binding: TimelapseArchiveFragmentBinding
    private val helper = CollapsibleToolbarTabsHelper()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        TimelapseArchiveFragmentBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helper.install(
            octoActivity = requireOctoActivity(),
            binding = binding.appBarLayout
        )
        helper.removeTabs()

        binding.appBarLayout.title.text = "Timelapse Archive**"
        binding.appBarLayout.subtitle.text =
            "OctoPrint let's you create timelapses of your prints. You can see the recordings below, manage your settings <a href=\"\">here</a>.**".toHtml()
        binding.appBarLayout.subtitle.movementMethod = LinkClickMovementMethod { _, _ ->
            MenuBottomSheetFragment.createForMenu(TimelapseMenu()).show(childFragmentManager)
            true
        }

        val adapter = TimelapseArchiveAdapter()
        binding.recycler.adapter = adapter
        binding.swipeLayout.setOnRefreshListener { viewModel.fetchLatest() }
        viewModel.viewState.observe(viewLifecycleOwner) {
            binding.errorState.isVisible = it is Error || (binding.errorState.isVisible && it is Loading)
            binding.recycler.isVisible = it is Idle || (binding.recycler.isVisible && it is Loading)
            binding.swipeLayout.isRefreshing = it is Loading
            binding.errorMessage.text = (it as? Error)?.exception?.composeErrorMessage(requireContext())
            binding.retry.setOnClickListener { viewModel.fetchLatest() }
        }
        viewModel.viewData.observe(viewLifecycleOwner) {
            adapter.items = it
            binding.errorState.isVisible = true
            binding.errorMessage.text = "No timelapses found**"
        }
    }

    override fun onResume() {
        super.onResume()
        helper.handleResume()
    }

    override fun handleInsets(insets: Rect) {
        helper.handleInsets(insets)
    }
}