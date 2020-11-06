package de.crysxd.octoapp.signin.troubleshoot

import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_trouble_shooting.*

class TroubleShootingFragment : Fragment(R.layout.fragment_trouble_shooting) {

    private val viewModel: TroubleShootViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navArgs by navArgs<TroubleShootingFragmentArgs>()
        val baseUrl = navArgs.baseUrl
        val apiKey = navArgs.apiKey

        viewModel.runTest(requireContext(), baseUrl, apiKey).observe(viewLifecycleOwner) {
            when (it) {
                is TroubleShootingResult.Running -> {
                    textViewState.text = "Running Check ${it.step} of ${it.totalSteps}"
                    textViewSubState.text = it.status
                    buttonMain.isVisible = false
                }

                is TroubleShootingResult.Failure -> {
                    textViewState.text = HtmlCompat.fromHtml(it.text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                    textViewSubState.text = it.suggestions.joinToString("<br/>") { c ->
                        "👉 $c"
                    }.let { s ->
                        HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                    buttonMain.text = "Check again"
                    buttonMain.isVisible = true
                    buttonMain.setOnClickListener { viewModel.runTest(requireContext(), baseUrl, apiKey) }
                }

                TroubleShootingResult.Success -> {
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