package de.crysxd.octoapp.help.plugins

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.help.databinding.HelpPluginsLibraryFragmentBinding
import de.crysxd.octoapp.help.di.injectViewModel

class PluginsLibraryFragment : BaseFragment(), InsetAwareScreen {
    override val viewModel by injectViewModel<PluginsLibraryViewModel>()
    private lateinit var binding: HelpPluginsLibraryFragmentBinding
    private var previousMediator: TabLayoutMediator? = null
    private var lastVerticalOffset = 0

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

        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            if (lastVerticalOffset == 0 || verticalOffset == 0) {
                val scrolled = verticalOffset != 0
                requireOctoActivity().octo.isVisible = !scrolled
                binding.toolbarContainer.animate().alpha(if (scrolled) 0f else 1f).start()
            }

            lastVerticalOffset = verticalOffset
        })

        var firstSelection = true
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (firstSelection) {
                    firstSelection = false
                } else {
                    binding.appBar.setExpanded(false, true)
                }
            }
        })

        viewModel.mutablePluginsIndex.observe(viewLifecycleOwner) {
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
        requireOctoActivity().octo.isVisible = lastVerticalOffset == 0
        requireOctoActivity().octoToolbar.isVisible = false
    }

    override fun handleInsets(insets: Rect) {
        binding.toolbarContainer.updatePadding(top = insets.top)
        binding.tabsContainer.updatePadding(top = insets.top)
        binding.tabsContainer.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        binding.toolbar.updateLayoutParams {
            height = binding.tabsContainer.measuredHeight
        }
    }
}
