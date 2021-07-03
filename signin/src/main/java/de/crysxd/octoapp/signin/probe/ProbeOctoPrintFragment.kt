package de.crysxd.octoapp.signin.probe

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.base.utils.ThemePlugin
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.ProbeFragmentFindingBinding
import de.crysxd.octoapp.signin.databinding.ProbeFragmentInitialBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.ext.goBackToDiscover
import io.noties.markwon.Markwon
import timber.log.Timber

class ProbeOctoPrintFragment : BaseFragment() {
    override val viewModel by injectViewModel<ProbeOctoPrintViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(Injector.get().viewModelFactory())
    private var loadingBinding: ProbeFragmentInitialBinding? = null
    private var findingBinding: ProbeFragmentFindingBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loading.subtitle.isVisible = false
        binding.loading.title.text = "Testing the connection to OctoPrint..."

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }

        viewModel.webUrl = navArgs<ProbeOctoPrintFragmentArgs>().value.baseUrl
        startProbe()
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                ProbeOctoPrintViewModel.UiState.Loading -> showLoading()
                is ProbeOctoPrintViewModel.UiState.FindingsReady -> {
                    if (it.finding == null) {
                        continueWithNoFindings()
                    } else {
                        showFinding(it.finding)
                    }
                }
            }
        }

        // Disable back button, we can't go back here
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackToDiscover()
        })
    }

    private fun startProbe() {
        viewModel.probe()
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(binding.root)

    private fun continueWithNoFindings() {
        findNavController().navigate(ProbeOctoPrintFragmentDirections.requestAccess(viewModel.webUrl))
    }

    private fun showLoading() {
        beginDelayedTransition()
        binding.octoView.swim()
        val b = loadingBinding ?: ProbeFragmentInitialBinding.inflate(LayoutInflater.from(requireContext()), binding.content, false)
        loadingBinding = b
        binding.content.removeAllViews()
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        binding.content.addView(b.root)
    }

    private fun showFinding(finding: TestFullNetworkStackUseCase.Finding) {
        beginDelayedTransition()
        binding.octoView.idle()
        val b = findingBinding ?: ProbeFragmentFindingBinding.inflate(LayoutInflater.from(requireContext()), binding.content, false)
        findingBinding = b
        binding.content.removeAllViews()
        binding.content.addView(b.root)

        val markwon = Markwon.builder(requireContext())
            .usePlugin(ThemePlugin(requireContext()))
            .build()

        markwon.setMarkdown(b.content, getExplainerForFinding(finding))
        markwon.setMarkdown(b.title, getTitleForFinding(finding))

        b.buttonEdit.text = getEditButtonText()
        b.buttonEdit.setOnClickListener { getEditButtonAction() }
        b.buttonContinue.setOnClickListener { performPrimaryAction(finding) }
        b.buttonContinue.text = getPrimaryActionText(finding)
        b.passwordInput.isVisible = finding is TestFullNetworkStackUseCase.Finding.BasicAuthRequired
        b.usernameInput.isVisible = b.passwordInput.isVisible
        b.passwordInput.editText.setOnEditorActionListener { _, _, _ ->
            performPrimaryAction(finding)
            true
        }
    }

    private fun getEditButtonAction() {
        if (Injector.get().octorPrintRepository().getActiveInstanceSnapshot() != null) {
            // Case A: We got here because a API key was invalid, there is an active instance
            goBackToDiscover()
        } else {
            // Case B: User is signing in, but nothing is active yet
            findNavController().popBackStack()
        }
    }

    private fun getEditButtonText() = if (Injector.get().octorPrintRepository().getActiveInstanceSnapshot() != null) {
        // Case A: We got here because a API key was invalid, there is an active instance
        "Connect to an other OctoPrint H"
    } else {
        // Case B: User is signing in, but nothing is active yet
        "Edit information H"
    }

    private fun getTitleForFinding(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> "Basic Authentication required"
        is TestFullNetworkStackUseCase.Finding.DnsFailure -> "Unable to resolve **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.LocalDnsFailure -> "Unable to resolve **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.HostNotReachable -> "Unable to connect to **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> "Android does not trust **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.InvalidUrl -> "URL syntax error"
        is TestFullNetworkStackUseCase.Finding.OctoPrintNotFound -> "OctoPrint not found"
        is TestFullNetworkStackUseCase.Finding.PortClosed -> "Connected to **${finding.host}** but port **${finding.port}** is closed"
        is TestFullNetworkStackUseCase.Finding.UnexpectedHttpIssue -> "Failed to connect to **${finding.host}** via HTTP"
        is TestFullNetworkStackUseCase.Finding.UnexpectedIssue -> "Unexpected issue"
        is TestFullNetworkStackUseCase.Finding.ServerMightNotBeOctoPrint -> "**${finding.host}** might not be an OctoPrint"
    }

    private fun getExplainerForFinding(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> "Please enter your Basic Auth credentials below, not your OctoPrint user and password."
        is TestFullNetworkStackUseCase.Finding.DnsFailure -> "Android is not able to resolve the IP address for **${finding.host}**. Check following things to resolve the issue:\n\n- Ensure there is no typo\n- Make sure you are connected to the internet\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser\n- Try using the **IP address** instead of **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.LocalDnsFailure -> "Android is not able to resolve the IP address for **${finding.host}**. Android devices are sometimes configured by the manufacturer in a way that they cannot reliably resolve .home or .local domains. Check following things to resolve the issue:\n\n- Ensure there is no typo\n- Make sure you are connected to the internet\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser\n- Try using the **IP address** instead of **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.HostNotReachable -> "OctoApp resolved the IP of **${finding.host}**, but **${finding.ip}** can't be reached within 2 seconds. Check following things to resolve the issue:\n\n- Make sure the machine hosting OctoPrint is turned on and connected\n- If your OctoPrint is only available locally, make sure you are connected to the correct WiFi network\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser"
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> if (finding.weakHostnameVerificationRequired) {
            "The SSL certificate presented by your OctoPrint can not be used to establish a HTTPS connection because Android does not trust the certificate. This is a common issue for self-signed certificates that were not installed on this phone.\n\nAdditionally, the certificate uses a deprecated format that does not comply to modern security standards. A \"subject alternative name\" (SAN) field is required from Android 9 onwards. \n\nOctoApp can add this certificate to a trusted list and will bypass Android’s check for this certificate."
        } else {
            "The SSL certificate presented by your OctoPrint can not be used to establish a HTTPS connection because Android does not trust the certificate. This is a common issue for self-signed certificates that were not installed on this phone.\n\nOctoApp can add this certificate to a trusted list and will bypass Android’s check for this certificate."
        }
        is TestFullNetworkStackUseCase.Finding.InvalidUrl -> "The URL **${finding.webUrl}** seems to contain a syntax error and can't be parsed. Android reports following error:\n\n**${finding.exception.localizedMessage ?: "Unknown error"}**"
        is TestFullNetworkStackUseCase.Finding.OctoPrintNotFound -> "OctoApp was able to connect to **${finding.webUrl}** but received a 404 response. Check following things to resolve the issue:\n\n- Make sure you use the correct port and host\n- Make sure that you use the correct path in the URL\n- Try to open [**${finding.webUrl}](**${finding.webUrl}**) in your browser"
        is TestFullNetworkStackUseCase.Finding.PortClosed -> "OctoApp was able to connect to **${finding.host}** but port **${finding.port}** is closed. Check following things to resolve the issue:\n\n- Make sure **${finding.port}** is correct. If you don't specify the port explicitly, OctoApp will use 80 for HTTP and 443 for HTTPS\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser"
        is TestFullNetworkStackUseCase.Finding.UnexpectedHttpIssue -> "OctoApp was able to communicate with **${finding.host}**, but when trying to establish a HTTP(S) connection an unexpected error occurred. Android reports following issue:\n\n**${finding.exception.localizedMessage ?: "Unknown error"}**"
        is TestFullNetworkStackUseCase.Finding.UnexpectedIssue -> "OctoApp encountered an unexpected error. Android reports following issue:\n\n**${finding.exception.localizedMessage ?: "Unknown error"}**"
        is TestFullNetworkStackUseCase.Finding.ServerMightNotBeOctoPrint -> "OctoApp was able to communicate with **${finding.host}**, but the server seems not to be a recent version of OctoPrint.\n\nYou can continue, but other issues may arise in the following steps."
    }

    private fun getPrimaryActionText(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> "Trust & Continue"
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired, is TestFullNetworkStackUseCase.Finding.ServerMightNotBeOctoPrint -> "Continue"
        else -> "Try again"
    }

    private fun performPrimaryAction(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> {
            Injector.get().sslKeyStoreHandler().also {
                it.storeCertificates(finding.certificates)
                if (finding.weakHostnameVerificationRequired) {
                    it.enforceWeakVerificationForHost(finding.webUrl)
                }
            }

            // Start again
            startProbe()
        }
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> {
            val user = findingBinding?.usernameInput?.editText?.text?.toString() ?: ""
            val password = findingBinding?.passwordInput?.editText?.text?.toString() ?: ""
            val uri = Uri.parse(finding.webUrl)
            val credentials = when {
                user.isNotBlank() && password.isNotBlank() -> "$user:$password@"
                user.isNotBlank() -> "$user@"
                else -> ""
            }
            viewModel.webUrl = uri.buildUpon().encodedAuthority("$credentials${uri.host}").build().toString()
            Injector.get().sensitiveDataMask().registerWebUrl(viewModel.webUrl, "octoprint")
            startProbe()
        }
        is TestFullNetworkStackUseCase.Finding.ServerMightNotBeOctoPrint -> continueWithNoFindings()
        else -> startProbe()
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }
}