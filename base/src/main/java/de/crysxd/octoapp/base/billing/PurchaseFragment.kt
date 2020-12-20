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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.utils.LongDuration
import kotlinx.android.synthetic.main.fragment_purchase.*
import kotlinx.android.synthetic.main.fragment_purchase_init_state.*
import kotlinx.android.synthetic.main.fragment_purchase_sku_state.*
import kotlinx.android.synthetic.main.fragment_purchase_sku_state_option.view.*
import kotlinx.android.synthetic.main.purchase_header.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.absoluteValue

class PurchaseFragment : BaseFragment(R.layout.fragment_purchase), InsetAwareScreen {

    override val viewModel: PurchaseViewModel by injectViewModel(Injector.get().viewModelFactory())
    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (viewModel.viewState.value) {
                is PurchaseViewModel.ViewState.SkuSelectionState -> viewModel.moveToInitState()
                else -> Unit
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
        viewModel.viewState.observe(viewLifecycleOwner, ::moveToState)

        var eventSent = false
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val collapseProgress = verticalOffset.absoluteValue / appBar.totalScrollRange.toFloat()
            val content = listOf<View?>(initState, skuState).firstOrNull { it?.isVisible == true }
            val padding = statusBarScrim.height + requireContext().resources.getDimension(R.dimen.margin_4)
            content?.updatePadding(top = (padding * collapseProgress).toInt())

            if (!eventSent && verticalOffset != 0) {
                eventSent = true
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenScroll)
            }
        })

        populateInitState()
    }

    private fun moveToState(state: PurchaseViewModel.ViewState) {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            val fullyExpanded = (appBar.height - appBar.bottom) == 0
            if (!fullyExpanded) {
                appBar.setExpanded(true)
                delay(300)
            }

            TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition(explode = true))
            initState?.isVisible = false
            skuState?.isVisible = false
            buttonSupport.isVisible = state is PurchaseViewModel.ViewState.InitState
            backPressedCallback.isEnabled = state != PurchaseViewModel.ViewState.InitState

            when (state) {
                PurchaseViewModel.ViewState.InitState -> {
                    skuList?.removeAllViews()
                    initState?.isVisible = true
                }

                is PurchaseViewModel.ViewState.SkuSelectionState -> {
                    skuStateStub?.isVisible = true
                    skuState?.isVisible = true
                    populateSkuState(state)
                }
            }
        }
    }

    private fun populateSkuState(state: PurchaseViewModel.ViewState.SkuSelectionState) {
        skuState.updatePadding(top = 0)
        skuList.removeAllViews()
        skuTitle.text = HtmlCompat.fromHtml(
            Firebase.remoteConfig.getString("sku_list_title"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        state.billingData.availableSku.forEach { details ->
            val view = View.inflate(requireContext(), R.layout.fragment_purchase_sku_state_option, null)
            view.price.text = details.price
            view.buttonSelect.text = state.names.getOrElse(details.sku) { details.title }
            view.details.text = LongDuration.parse(details.freeTrialPeriod)?.format(requireContext())?.let {
                getString(R.string.free_trial_x, it)
            }
            view.details.isVisible = !view.details.text.isNullOrBlank()
            view.buttonSelect.setOnClickListener {
                BillingManager.purchase(requireActivity(), details)
            }
            view.badge.setImageResource(
                when (state.badges[details.sku]) {
                    PurchaseViewModel.Badge.NoBadge -> 0
                    PurchaseViewModel.Badge.Popular -> R.drawable.ic_badge_popular
                    PurchaseViewModel.Badge.BestValue -> R.drawable.ic_badge_best_value
                    null -> 0
                }
            )
            skuList.addView(view)
        }
    }

    private fun populateInitState() {
        initState.updatePadding(top = 0)

        purchaseTitle.text = HtmlCompat.fromHtml(
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

        purchaseTitle.movementMethod = LinkMovementMethod()
        description.movementMethod = LinkMovementMethod()
        featureList.movementMethod = LinkMovementMethod()
        moreFeatures.movementMethod = LinkMovementMethod()
        buttonSupport.setOnClickListener {
            viewModel.moveToSkuListState()
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false

        lifecycleScope.launchWhenStarted {
            // Billing completed, pop back stack and return
            BillingManager.billingEventFlow().collectLatest {
                findNavController().popBackStack()
            }
        }
    }

    override fun handleInsets(insets: Rect) {
        scrollView.setPadding(insets.left, 0, insets.right, 0)
        container.updatePadding(bottom = insets.bottom)
        statusBarScrim.updateLayoutParams { height = insets.top }
        imageViewStatusBackground.updateLayoutParams { height = insets.top }
    }
}