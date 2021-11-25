package de.crysxd.baseui.utils

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import de.crysxd.baseui.OctoActivity

class CollapsibleToolbarTabsHelper {

    private lateinit var toolbar: Toolbar
    private lateinit var octoActivity: OctoActivity
    private lateinit var tabsContainer: ViewGroup
    private lateinit var toolbarContainer: ViewGroup
    private var lastVerticalOffset = 0
    private var createdAt = System.currentTimeMillis()

    fun install(
        octoActivity: OctoActivity,
        appBar: AppBarLayout,
        toolbar: Toolbar,
        tabsContainer: ViewGroup,
        toolbarContainer: ViewGroup,
        tabs: TabLayout,
    ) {
        this.toolbar = toolbar
        this.tabsContainer = tabsContainer
        this.toolbarContainer = toolbarContainer
        this.octoActivity = octoActivity

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            if (lastVerticalOffset == 0 || verticalOffset == 0) {
                val scrolled = verticalOffset != 0
                octoActivity.octo.isVisible = !scrolled
                toolbarContainer.animate().alpha(if (scrolled) 0f else 1f).start()
            }

            lastVerticalOffset = verticalOffset
        })


        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (System.currentTimeMillis() - createdAt > 1000) {
                    appBar.setExpanded(false, true)
                }
            }
        })
    }

    fun markTabsCreated() {
        createdAt = System.currentTimeMillis()
    }

    fun handleResume() {
        octoActivity.octo.isVisible = lastVerticalOffset == 0
        octoActivity.octoToolbar.isVisible = false
    }

    fun handleInsets(insets: Rect) {
        toolbarContainer.updatePadding(top = insets.top)
        tabsContainer.updatePadding(top = insets.top)
        tabsContainer.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        toolbar.updateLayoutParams {
            height = tabsContainer.measuredHeight
        }
    }
}