package de.crysxd.octoapp.signin.troubleshoot

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
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
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_trouble_shooting.*

class TroubleShootingFragment : Fragment(R.layout.fragment_trouble_shooting) {

    private val viewModel: TroubleShootViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navArgs by navArgs<TroubleShootingFragmentArgs>()
        val baseUrl = navArgs.baseUrl
        val apiKey = navArgs.apiKey

        buttonMain.setOnLongClickListener {
            SendFeedbackDialog().show(childFragmentManager, "send-feedback")
            true
        }

        viewModel.runTest(requireContext(), baseUrl, apiKey).observe(viewLifecycleOwner) {
            suggestionsContainer.removeAllViews()
            buttonMain.isVisible = false
            buttonSupport.isVisible = false
            buttonDetails.isVisible = false

            TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition(explode = true))

            when (it) {
                is TroubleShootingResult.Running -> {
                    octoBackground.isVisible = true
                    octoView.isVisible = true

                    textViewState.text = "Running Check ${it.step} of ${it.totalSteps}"
                    textViewSubState.text = it.status
                }

                is TroubleShootingResult.Failure -> {
                    octoBackground.isVisible = false
                    octoView.isVisible = false

                    textViewState.text = HtmlCompat.fromHtml(it.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    textViewSubState.text = HtmlCompat.fromHtml(it.description, HtmlCompat.FROM_HTML_MODE_LEGACY)
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
                        suggestionsContainer.addView(it)
                    }
                    buttonMain.text = "Check again"
                    buttonMain.isVisible = true
                    buttonMain.setOnClickListener { viewModel.runTest(requireContext(), baseUrl, apiKey) }
                    buttonDetails.isVisible = it.exception != null
                    buttonDetails.setOnClickListener { _ ->
                        it.exception?.let { e -> requireOctoActivity().showErrorDetailsDialog(e, false) }
                    }
                    buttonSupport.isVisible = it.offerSupport
                    buttonSupport.setOnClickListener {
                        Firebase.analytics.logEvent("troubleshoot_failure_support_trigger", Bundle.EMPTY)
                        SendFeedbackDialog().show(childFragmentManager, "send-feedback")
                    }
                }

                TroubleShootingResult.Success -> {
                    octoBackground.isVisible = false
                    octoView.isVisible = false

                    textViewState.text = "All checks passed"
                    textViewSubState.text = "Your settings seem correct"
                    buttonMain.text = "Go back"
                    buttonMain.isVisible = true
                    buttonMain.setOnClickListener { findNavController().popBackStack() }
                }
            }
        }
    }
}