package de.crysxd.octoapp.help.plugins

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.CollapsibleToolbarTabsHelper
import de.crysxd.octoapp.help.databinding.HelpPluginsLibraryFragmentBinding
import de.crysxd.octoapp.help.di.injectViewModel

class PluginsLibraryFragment : BaseFragment(), InsetAwareScreen {
    override val viewModel by injectViewModel<PluginsLibraryViewModel>()
    private lateinit var binding: HelpPluginsLibraryFragmentBinding
    private var previousMediator: TabLayoutMediator? = null
    private val helper = CollapsibleToolbarTabsHelper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = HelpPluginsLibraryFragmentBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PluginsLibraryPagerAdapter()
        binding.viewPager.adapter = adapter

        helper.install(
            octoActivity = requireOctoActivity(),
            tabs = binding.tabs,
            tabsContainer = binding.tabsContainer,
            appBar = binding.appBar,
            toolbar = binding.toolbar,
            toolbarContainer = binding.toolbarContainer
        )

        viewModel.pluginsIndex.observe(viewLifecycleOwner) {
            helper.markTabsCreated()
            createTabs(it)
            adapter.index = it
        }
    }

    private fun createTabs(index: PluginsLibraryViewModel.PluginsIndex) {
        previousMediator?.detach()
        previousMediator = TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = index.categories[position].name
        }
        previousMediator?.attach()
    }

    override fun onResume() {
        super.onResume()
        helper.handleResume()
    }

    override fun handleInsets(insets: Rect) {
        helper.handleInsets(insets)
    }
}
