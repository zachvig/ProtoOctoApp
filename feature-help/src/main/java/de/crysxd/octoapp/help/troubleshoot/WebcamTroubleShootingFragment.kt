package de.crysxd.octoapp.help.troubleshoot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.feedback.SendFeedbackDialog
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.base.utils.ThemePlugin
import de.crysxd.octoapp.help.R
import de.crysxd.octoapp.help.databinding.HelpWebcamTroubleshootingBinding
import de.crysxd.octoapp.help.di.injectViewModel
import io.noties.markwon.Markwon

class WebcamTroubleShootingFragment : BaseFragment() {
    override val viewModel: WebcamTroubleShootingViewModel by injectViewModel()
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
        showLoadingState()
        viewModel.uiState.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            when (it) {
                is WebcamTroubleShootingViewModel.UiState.Finding -> showFinding(it.finding)
                WebcamTroubleShootingViewModel.UiState.Loading -> showLoadingState()
                WebcamTroubleShootingViewModel.UiState.UnsupportedWebcam -> {
                    requireOctoActivity().showDialog(message = getString(R.string.help___webcam_troubleshooting___only_mjpeg_supported))
                    findNavController().popBackStack()
                }
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
                binding.buttonContinue.setOnClickListener { findNavController().popBackStack() }
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