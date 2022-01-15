package de.crysxd.baseui.utils

import android.graphics.Rect
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.databinding.CollapsibleToolbarTabsLayoutBinding

class CollapsibleToolbarTabsHelper {

    private lateinit var binding: CollapsibleToolbarTabsLayoutBinding
    private lateinit var octoActivity: OctoActivity
    private lateinit var viewLifecycleOwner: LifecycleOwner
    private var lastVerticalOffset = 0
    private var createdAt = System.currentTimeMillis()

    fun install(
        octoActivity: OctoActivity,
        binding: CollapsibleToolbarTabsLayoutBinding,
        viewLifecycleOwner: LifecycleOwner,
        showOctoInToolbar: Boolean = true
    ) {
        this.binding = binding
        this.octoActivity = octoActivity
        this.viewLifecycleOwner = viewLifecycleOwner

        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            if (lastVerticalOffset == 0 || verticalOffset == 0) {
                val scrolled = verticalOffset != 0
                octoActivity.octo.isVisible = !scrolled && showOctoInToolbar
                binding.toolbarContainer.animate().alpha(if (scrolled) 0f else 1f).start()
            }

            lastVerticalOffset = verticalOffset
        })


        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (System.currentTimeMillis() - createdAt > 1000) {
                    binding.appBar.setExpanded(false, true)
                }
            }
        })

        octoActivity.controlCenter.disableForLifecycle(viewLifecycleOwner.lifecycle)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                octoActivity.octo.isVisible = lastVerticalOffset == 0 && showOctoInToolbar
                octoActivity.octoToolbar.isVisible = false
            }
        })
    }

    fun removeTabs() {
        binding.toolbar.updateLayoutParams { height = 0 }
        binding.tabsContainer.isVisible = false
    }

    fun markTabsCreated() {
        createdAt = System.currentTimeMillis()
    }

    fun handleInsets(insets: Rect) {
        binding.toolbarContainer.updatePadding(top = insets.top)
        binding.tabsContainer.updatePadding(top = insets.top)
        binding.tabsContainer.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        binding.toolbar.updateLayoutParams {
            height = binding.tabsContainer.measuredHeight.takeIf { binding.tabsContainer.isVisible } ?: insets.top
        }
    }
}