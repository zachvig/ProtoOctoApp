package de.crysxd.octoapp.base.billing

import android.graphics.Rect
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.android.synthetic.main.fragment_purchase.*
import kotlinx.android.synthetic.main.fragment_purchase_init_state.*
import kotlinx.android.synthetic.main.fragment_purchase_sku_state.*
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.math.max

class PurchaseFragment : Fragment(R.layout.fragment_purchase), InsetAwareScreen {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title.text = HtmlCompat.fromHtml(
            Firebase.remoteConfig.getString("purchase_screen_title"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        description.text = HtmlCompat.fromHtml(
            Firebase.remoteConfig.getString("purchase_screen_description"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        featureList.text = HtmlCompat.fromHtml(
            Firebase.remoteConfig.getString("purchase_screen_features"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        moreFeatures.text = HtmlCompat.fromHtml(
            Firebase.remoteConfig.getString("purchase_screen_more_features"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        title.movementMethod = LinkMovementMethod()
        description.movementMethod = LinkMovementMethod()
        featureList.movementMethod = LinkMovementMethod()
        moreFeatures.movementMethod = LinkMovementMethod()

        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                startDelayedTransition()
                initState.isVisible = true
                buttonSupport.isVisible = true
                skuState?.isVisible = false
                appBar.setExpanded(true)
                isEnabled = false
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        buttonSupport.setOnClickListener {
            startDelayedTransition()
            buttonSupport.isVisible = false
            initState.isVisible = false
            skuStateStub?.isVisible = true
            skuState.isVisible = true
            appBar.setExpanded(true)
            backPressedCallback.isEnabled = true
        }

        var eventSent = false
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            appBar.post {
                val collapseProgress = verticalOffset.absoluteValue / appBar.totalScrollRange.toFloat()
                val content = if (initState.isVisible) initState else skuState
                val availableHeight = (scrollView.height - content.height - content.paddingTop - content.paddingBottom) / 2

                if (!eventSent && verticalOffset != 0) {
                    eventSent = true
                    OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenScroll)
                }

                // Top padding should be at least as much as bottom padding
                val top = max(content.paddingBottom, availableHeight)
                Timber.i("$top ${top * collapseProgress}")

                content.updatePadding(
                    top = (top * collapseProgress).toInt()
                )
            }
        })
    }

    private fun startDelayedTransition() = TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition(explode = true))

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