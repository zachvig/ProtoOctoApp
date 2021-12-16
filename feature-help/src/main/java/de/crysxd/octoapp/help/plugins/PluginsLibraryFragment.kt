package de.crysxd.octoapp.help.plugins

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.CollapsibleToolbarTabsHelper
import de.crysxd.octoapp.help.databinding.HelpPluginsLibraryFragmentBinding
import de.crysxd.octoapp.help.di.injectViewModel
import java.util.concurrent.TimeUnit

class PluginsLibraryFragment : BaseFragment(), InsetAwareScreen {
    override val viewModel by injectViewModel<PluginsLibraryViewModel>()
    private lateinit var binding: HelpPluginsLibraryFragmentBinding
    private var previousMediator: TabLayoutMediator? = null
    private val helper = CollapsibleToolbarTabsHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)
    }

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
            startPostponedEnterTransition()
            helper.markTabsCreated()
            createTabs(it)
            adapter.index = it

            binding.tabs.post {
                val selectedCategory = navArgs<PluginsLibraryFragmentArgs>().value.category?.takeIf { c -> c.isNotBlank() }
                val selectedIndex = it.categories.indexOfFirst { c -> c.id == selectedCategory }.takeIf { i -> i >= 0 } ?: 0
                binding.tabs.selectTab(binding.tabs.getTabAt(selectedIndex))
            }
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
