package de.crysxd.octoapp.base.billing

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.android.synthetic.main.fragment_purchase.*
import kotlin.math.absoluteValue

class PurchaseFragment : Fragment(R.layout.fragment_purchase), InsetAwareScreen {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        description.text = HtmlCompat.fromHtml(getString(R.string.purchase_expalainer), HtmlCompat.FROM_HTML_MODE_LEGACY)
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val collapseProgress = verticalOffset.absoluteValue / appBar.totalScrollRange.toFloat()
            val availableHeight = (scrollView.height - scrollContent.height - scrollContent.paddingTop) / 2

            // Top padding should be at least as much as bottom padding
            val minPaddingTop = scrollContent.paddingBottom

            // If we don't need to scroll, center content while scrolling
            // If we need to scroll, fade in the top padding
            if (availableHeight > minPaddingTop) {
                scrollContent.translationY = availableHeight * collapseProgress
            } else {
                scrollContent.updatePadding(
                    top = (minPaddingTop * collapseProgress).toInt()
                )
            }
        })
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false
    }

    override fun handleInsets(insets: Rect) {
        scrollView.setPadding(insets.left, 0, insets.right, 0)
        container.updatePadding(bottom = insets.bottom)
        statusBarScrim.updateLayoutParams { height = insets.top }
        imageViewStatusBackground.updateLayoutParams { height = insets.top }
    }
}