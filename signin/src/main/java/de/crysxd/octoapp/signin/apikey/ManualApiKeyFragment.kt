package de.crysxd.octoapp.signin.apikey

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.access.RequestAccessFragmentDirections
import de.crysxd.octoapp.signin.apikey.ReadQrCodeFragment.Companion.RESULT_API_KEY
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentContentManualBinding
import de.crysxd.octoapp.signin.di.Injector
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.ext.setUpAsHelpButton
import kotlinx.coroutines.delay

class ManualApiKeyFragment : BaseFragment() {
    private lateinit var binding: BaseSigninFragmentBinding
    private lateinit var contentBinding: DiscoverFragmentContentManualBinding
    override val viewModel: ManualApiKeyViewModel by injectViewModel(
        Injector.get().viewModelFactory()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(inflater, container, false).also {
            binding = it
            it.content.removeAllViews()
            contentBinding = DiscoverFragmentContentManualBinding.inflate(inflater, it.content, true)
            it.octoView.isVisible = false
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAsHelpButton(contentBinding.help)
        OctoAnalytics.logEvent(OctoAnalytics.Event.ManualApiKeyUsed)
        contentBinding.input.hintNormal = getString(R.string.sign_in___manual_api_key___hint)
        contentBinding.input.hintActive = getString(R.string.sign_in___manual_api_key___hint_active)
        contentBinding.input.actionIcon = R.drawable.ic_qr_code_scanner_24px
        contentBinding.title.setText(R.string.sign_in___manual_api_key___title)
        contentBinding.input.examples = listOf("076B961EEB9146F1AE7B3AF2ED1E3AD8", "3219DEEC978F49DF9B426CD42E793CE9", "FA7B1CF230124157A5DCC6322EEC1E9A")
        contentBinding.input.setOnActionListener {
            findNavController().navigate(R.id.action_read_qr_code)
        }
        contentBinding.input.editText.setOnEditorActionListener { _, _, _ ->
            contentBinding.buttonContinue.performClick()
        }
        contentBinding.buttonContinue.setOnClickListener {
            viewModel.testApiKey(
                webUrl = navArgs<ManualApiKeyFragmentArgs>().value.webUrl,
                apiKey = contentBinding.input.editText.text.toString(),
            )
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData(RESULT_API_KEY, "")?.observe(viewLifecycleOwner) {
            if (it.isNotBlank()) {
                contentBinding.input.editText.setText(it)
                contentBinding.buttonContinue.performClick()
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            contentBinding.buttonContinue.isEnabled = it !is ManualApiKeyViewModel.ViewState.Loading
            contentBinding.buttonContinue.setText(
                if (contentBinding.buttonContinue.isEnabled) {
                    R.string.sign_in___continue
                } else {
                    R.string.loading
                }
            )

            when (it) {
                is ManualApiKeyViewModel.ViewState.InvalidApiKey -> if (!it.handled) {
                    it.handled = true
                    requireOctoActivity().showDialog(
                        message = getString(R.string.sign_in___manual_api_key___error_invalid_api_key)
                    )
                }

                is ManualApiKeyViewModel.ViewState.UnexpectedError -> if (!it.handled) {
                    it.handled = true
                    requireOctoActivity().showDialog(
                        message = getString(R.string.sign_in___manual_api_key___error_other),
                        neutralButton = getString(R.string.sign_in___manual_api_key___troubleshooting),
                        neutralAction = {
                            findNavController().popBackStack(R.id.probeOctoPrintFragment, false)
                        }
                    )
                }

                is ManualApiKeyViewModel.ViewState.Success -> {
                    val extras = FragmentNavigatorExtras(binding.octoView to "octoView", binding.octoBackground to "octoBackground")
                    val directions = RequestAccessFragmentDirections.actionSuccess(webUrl = it.webUrl, apiKey = it.apiKey)
                    findNavController().navigate(directions, extras)
                }

                else -> Unit
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(600)
            contentBinding.input.showSoftKeyboard()
        }
    }

    override fun onStop() {
        super.onStop()
        contentBinding.input.hideSoftKeyboard()
    }
}