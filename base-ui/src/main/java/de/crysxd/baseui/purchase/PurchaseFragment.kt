package de.crysxd.baseui.purchase

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.R
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.databinding.PurchaseFragmentBinding
import de.crysxd.baseui.databinding.PurchaseFragmentSkuStateOptionBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.PurchaseOffers
import de.crysxd.octoapp.base.ext.purchaseOffers
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.utils.LongDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import kotlin.math.absoluteValue

class PurchaseFragment : BaseFragment(), InsetAwareScreen {

    private lateinit var binding: PurchaseFragmentBinding
    override val viewModel: PurchaseViewModel by injectViewModel()
    private var purchaseCompleted = false
    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (viewModel.viewState.value) {
                is PurchaseViewModel.ViewState.SkuSelectionState -> viewModel.moveToInitState()
                else -> Unit
            }
        }
    }
    private val config by lazy { Firebase.remoteConfig.purchaseOffers.activeConfig }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        PurchaseFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
        viewModel.viewState.observe(viewLifecycleOwner, ::moveToState)

        var eventSent = false
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val collapseProgress = verticalOffset.absoluteValue / binding.appBar.totalScrollRange.toFloat()
            val content = binding.saleBanner.takeIf { it.isVisible } ?: binding.contentContainer
            val padding = binding.statusBarScrim.height
            binding.statusBarScrim.alpha = 1 - collapseProgress
            content.updatePadding(top = (padding * collapseProgress).toInt())

            if (!eventSent && verticalOffset != 0) {
                eventSent = true
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenScroll)
            }
        })

        // Sales banner with countdown
        binding.saleBannerText.text = config.textsWithData.highlightBanner?.toHtml()
        binding.saleBanner.isVisible = binding.saleBannerText.text.isNotBlank()
        if (binding.saleBanner.isVisible) {
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                while (isActive) {
                    delay(1000)
                    binding.saleBannerText.text = config.textsWithData.highlightBanner?.toHtml()
                }
            }
        }

        populateInitState()
    }

    private fun moveToState(state: PurchaseViewModel.ViewState) {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            val fullyExpanded = (binding.appBar.height - binding.appBar.bottom) == 0
            if (!fullyExpanded) {
                binding.appBar.setExpanded(true)
                delay(300)
            }

            TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition(explode = true))
            binding.initState.root.isVisible = false
            binding.skuState.root.isVisible = false
            binding.unsupportedPlatformState.root.isVisible = false
            binding.buttonSupport.isVisible = state is PurchaseViewModel.ViewState.InitState
            backPressedCallback.isEnabled = !listOf(PurchaseViewModel.ViewState.InitState, PurchaseViewModel.ViewState.Unsupported).contains(state)

            when (state) {
                PurchaseViewModel.ViewState.Unsupported -> {
                    binding.unsupportedPlatformState.root.isVisible = true
                }

                PurchaseViewModel.ViewState.InitState -> {
                    OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseIntroShown)
                    binding.skuState.skuList.removeAllViews()
                    binding.initState.root.isVisible = true
                }

                is PurchaseViewModel.ViewState.SkuSelectionState -> {
                    OctoAnalytics.logEvent(
                        OctoAnalytics.Event.PurchaseOptionsShown, bundleOf(
                            "title" to binding.initState.purchaseTitle.text.take(100),
                            "text" to binding.initState.description.text.take(100),
                            "more_text" to binding.initState.moreFeatures.text.take(100),
                            "feature_text" to binding.initState.featureList.text.take(100),
                            "button_text" to binding.buttonSupport.text
                        )
                    )
                    binding.skuState.root.isVisible = true
                    populateSkuState(state)
                }
            }
        }
    }

    private fun populateSkuState(state: PurchaseViewModel.ViewState.SkuSelectionState) {
        binding.skuState.root.updatePadding(top = 0)
        binding.skuState.skuList.removeAllViews()
        binding.skuState.skuTitle.text = HtmlCompat.fromHtml(
            Firebase.remoteConfig.getString("sku_list_title"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        config.offers?.mapNotNull { offer ->
            val sku = state.billingData.allSku.firstOrNull { it.sku == offer.key } ?: return@mapNotNull null
            sku to offer.value
        }?.forEach { x ->
            val (details, offer) = x
            val itemBinding = PurchaseFragmentSkuStateOptionBinding.inflate(LayoutInflater.from(requireContext()))
            val dealFor = state.billingData.allSku.firstOrNull { it.sku == offer.dealFor }

            // If we have a matching offer, we show the old price
            dealFor?.let {
                itemBinding.priceOld.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                itemBinding.priceOld.text = it.price
                itemBinding.discount.text = NumberFormat.getPercentInstance().format((1 - (details.priceAmountMicros / it.priceAmountMicros.toFloat())) * -1)
            }
            itemBinding.priceOld.isVisible = itemBinding.priceOld.text.isNotBlank()
            itemBinding.discount.isVisible = itemBinding.priceOld.isVisible

            // Normal offer
            itemBinding.price.text = details.price
            itemBinding.buttonSelect.text = offer.label ?: details.title
            itemBinding.details.text = LongDuration.parse(details.freeTrialPeriod)?.format(requireContext())?.let { getString(R.string.free_trial_x, it) }
            itemBinding.details.isVisible = !itemBinding.details.text.isNullOrBlank()
            itemBinding.buttonSelect.setOnClickListener {
                OctoAnalytics.logEvent(
                    OctoAnalytics.Event.PurchaseOptionSelected, bundleOf(
                        "button_text" to itemBinding.buttonSelect.text,
                        "title" to binding.skuState.skuTitle.text,
                        "trial" to details.freeTrialPeriod,
                        "badge" to offer.badge,
                        "deal_for" to dealFor?.sku,
                        "sku" to details.sku
                    )
                )
                BillingManager.purchase(requireActivity(), details)
            }
            itemBinding.badge.setImageResource(
                when {
                    dealFor != null -> R.drawable.ic_badge_sale
                    offer.badge == PurchaseOffers.Badge.BestValue -> R.drawable.ic_badge_best_value
                    offer.badge == PurchaseOffers.Badge.Popular -> R.drawable.ic_badge_popular
                    else -> 0
                }
            )
            binding.skuState.skuList.addView(itemBinding.root)
        }

        if (state.billingData.allSku.isEmpty()) {
            OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseMissingSku)
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("Thanks for your interest! There was a issue loading available offers, check back later!")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    findNavController().popBackStack()
                }.show()
        }
    }

    private fun populateInitState() {
        val initBinding = binding.initState
        val texts = config.textsWithData
        initBinding.root.updatePadding(top = 0)

        initBinding.purchaseTitle.text = texts.purchaseScreenTitle.toHtml()
        initBinding.description.text = texts.purchaseScreenDescription.toHtml()
        initBinding.featureList.text = texts.purchaseScreenFeatures.toHtml()
        initBinding.moreFeatures.text = texts.purchaseScreenMoreFeatures.toHtml()
        binding.buttonSupport.text = texts.purchaseScreenContinueCta.toHtml()

        initBinding.purchaseTitle.movementMethod = LinkMovementMethod()
        initBinding.description.movementMethod = LinkMovementMethod()
        initBinding.featureList.movementMethod = LinkMovementMethod()
        initBinding.moreFeatures.movementMethod = LinkMovementMethod()
        binding.buttonSupport.setOnClickListener {
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
                if (it.isRecent()) {
                    purchaseCompleted = true
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!purchaseCompleted) {
            OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenClosed)
        }
    }

    override fun handleInsets(insets: Rect) {
        binding.scrollView.setPadding(insets.left, 0, insets.right, 0)
        binding.container.updatePadding(bottom = insets.bottom)
        binding.statusBarScrim.updateLayoutParams { height = insets.top }
        binding.header.imageViewStatusBackground.updateLayoutParams { height = insets.top }
    }
}