package de.crysxd.octoapp.base.ui.common.troubleshoot

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.databinding.TroubleShootingFragmentBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition

class TroubleShootingFragment : Fragment(R.layout.trouble_shooting_fragment) {

    private lateinit var binding: TroubleShootingFragmentBinding
    private val viewModel: TroubleShootViewModel by injectViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        TroubleShootingFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navArgs by navArgs<TroubleShootingFragmentArgs>()
        val baseUrl = navArgs.baseUrl
        val apiKey = navArgs.apiKey

        binding.buttonMain.setOnLongClickListener {
            UriLibrary.getHelpUri().open(requireOctoActivity())
            true
        }

        viewModel.runTest(requireContext(), baseUrl, apiKey).observe(viewLifecycleOwner) {
            binding.suggestionsContainer.removeAllViews()
            binding.buttonMain.isVisible = false
            binding.buttonSupport.isVisible = false
            binding.buttonDetails.isVisible = false

            TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition(explode = true))

            when (it) {
                is TroubleShootingResult.Running -> {
                    binding.octoBackground.isVisible = true
                    binding.octoView.isVisible = true

                    binding.textViewState.text = getString(R.string.running_check_x_of_y, it.step, it.totalSteps)
                    binding.textViewSubState.text = it.status
                }

                is TroubleShootingResult.Failure -> {
                    binding.octoBackground.isVisible = false
                    binding.octoView.isVisible = false

                    binding.textViewState.text = HtmlCompat.fromHtml(it.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.textViewSubState.text = HtmlCompat.fromHtml(it.description, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    it.suggestions.map {
                        val v = TextView(requireContext())
                        v.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        v.compoundDrawablePadding = requireContext().resources.getDimensionPixelSize(R.dimen.margin_1)
                        v.updatePadding(bottom = requireContext().resources.getDimensionPixelSize(R.dimen.margin_1))
                        v.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        v.movementMethod = LinkMovementMethod()
                        v.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_sentiment_satisfied_24, 0, 0, 0)
                        v
                    }.forEach {
                        binding.suggestionsContainer.addView(it)
                    }
                    binding.buttonMain.text = getString(R.string.check_again)
                    binding.buttonMain.isVisible = true
                    binding.buttonMain.setOnClickListener { viewModel.runTest(requireContext(), baseUrl, apiKey) }
                    binding.buttonDetails.isVisible = it.exception != null
                    binding.buttonDetails.setOnClickListener { _ ->
                        it.exception?.let { e -> requireOctoActivity().showErrorDetailsDialog(e, false) }
                    }
                    binding.buttonSupport.isVisible = it.offerSupport
                    binding.buttonSupport.setOnClickListener {
                        OctoAnalytics.logEvent(OctoAnalytics.Event.TroubleShootFailureSupportTrigger)
                        UriLibrary.getHelpUri().open(requireOctoActivity())
                    }
                }

                TroubleShootingResult.Success -> {
                    binding.octoBackground.isVisible = false
                    binding.octoView.isVisible = false

                    binding.textViewState.text = getString(R.string.all_checks_passed)
                    binding.textViewSubState.text = getString(R.string.settings_seem_correct)
                    binding.buttonMain.text = getString(R.string.go_back)
                    binding.buttonMain.isVisible = true
                    binding.buttonMain.setOnClickListener { findNavController().popBackStack() }
                }
            }
        }
    }
}