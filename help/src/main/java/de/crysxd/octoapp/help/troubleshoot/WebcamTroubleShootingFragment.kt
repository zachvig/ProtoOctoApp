package de.crysxd.octoapp.help.troubleshoot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.base.utils.ThemePlugin
import de.crysxd.octoapp.help.R
import de.crysxd.octoapp.help.databinding.HelpWebcamTroubleshootingBinding
import de.crysxd.octoapp.help.di.Injector
import de.crysxd.octoapp.help.di.injectViewModel
import io.noties.markwon.Markwon
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class WebcamTroubleShootingFragment : BaseFragment() {
    override val viewModel: WebcamTroubleShootingViewModel by injectViewModel(Injector.get().viewModelFactory())
    private lateinit var binding: HelpWebcamTroubleshootingBinding
    private val findingDescriptionLibrary by lazy { FindingDescriptionLibrary(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        HelpWebcamTroubleshootingBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.help.setOnClickListener {
            UriLibrary.getHelpUri().open(requireOctoActivity())
        }
        binding.help.setOnLongClickListener {
            SendFeedbackDialog().show(childFragmentManager, "webcam-feedback")
            true
        }
        viewModel.uiState.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            when (it) {
                is WebcamTroubleShootingViewModel.UiState.Finding -> showFinding(it.finding)
                WebcamTroubleShootingViewModel.UiState.Loading -> showLoadingState()
            }
        }
    }

    private fun showLoadingState() {
        binding.finding.isVisible = false
        binding.loading.isVisible = true
        binding.octoView.scheduleAnimation(600) {
            binding.octoView.swim()
        }
    }

    private fun showFinding(finding: TestFullNetworkStackUseCase.Finding) {
        binding.finding.isVisible = true
        binding.loading.isVisible = false
        binding.webcamViewContainer.isVisible = false
        binding.octoView.idle()

        val markwon = Markwon.builder(requireContext())
            .usePlugin(ThemePlugin(requireContext()))
            .build()

        markwon.setMarkdown(binding.content, findingDescriptionLibrary.getExplainerForFinding(finding))
        markwon.setMarkdown(binding.title, findingDescriptionLibrary.getTitleForFinding(finding))

        when (finding) {
            is TestFullNetworkStackUseCase.Finding.WebcamReady -> {
                binding.buttonContinue.text = getString(R.string.help___webcam_troubleshooting___continue)
                binding.webcamViewContainer.isVisible = true
                binding.webcamView.setImageBitmap(finding.image)
            }

            is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> {
                binding.buttonContinue.text = getString(R.string.help___webcam_troubleshooting___trust_and_try_again)
                binding.buttonContinue.setOnClickListener {
                    BaseInjector.get().sslKeyStoreHandler().storeCertificates(finding.certificates)
                    if (finding.weakHostnameVerificationRequired) {
                        BaseInjector.get().sslKeyStoreHandler().enforceWeakVerificationForHost(finding.webUrl)
                    }
                    viewModel.retry()
                }
            }

            else -> {
                binding.buttonContinue.text = getString(R.string.help___webcam_troubleshooting___try_again)
                binding.buttonContinue.setOnClickListener { viewModel.retry() }
            }
        }
    }
}