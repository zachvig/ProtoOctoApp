package de.crysxd.octoapp.signin.probe

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.base.utils.ThemePlugin
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.ProbeFragmentFindingBinding
import de.crysxd.octoapp.signin.databinding.ProbeFragmentInitialBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.ext.goBackToDiscover
import de.crysxd.octoapp.signin.ext.setUpAsHelpButton
import io.noties.markwon.Markwon
import timber.log.Timber

class ProbeOctoPrintFragment : BaseFragment() {
    override val viewModel by injectViewModel<ProbeOctoPrintViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(Injector.get().viewModelFactory())
    private var loadingBinding: ProbeFragmentInitialBinding? = null
    private var findingBinding: ProbeFragmentFindingBinding? = null
    private val initialWebUrl by lazy {
        navArgs<ProbeOctoPrintFragmentArgs>().value.baseUrl
    }
    private val allowApiKeyReuse by lazy {
        navArgs<ProbeOctoPrintFragmentArgs>().value.allowApiKeyReuse == true.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loading.subtitle.isVisible = false
        binding.loading.title.text = "Testing the connection to OctoPrint... H"

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }

        val webUrl = viewModel.lastWebUrl ?: initialWebUrl
        viewModel.probe(webUrl)
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                ProbeOctoPrintViewModel.UiState.Loading -> showLoading()
                is ProbeOctoPrintViewModel.UiState.FindingsReady -> {
                    when (it.finding) {
                        is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> continueWithPresentApiKey(it.finding)
                        is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> continueToRequestApiKey(it.finding.webUrl)
                        else -> showFinding(it.finding)
                    }
                }
            }
        }

        // Disable back button, we can't go back here
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackToDiscover()
        })
    }


    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(binding.root)

    private fun continueToRequestApiKey(webUrl: String) = binding.octoView.doAfterAnimation {
        // Octo is now in a neutral position, we can animate states
        val extras = FragmentNavigatorExtras(binding.octoView to "octoView", binding.octoBackground to "octoBackground")
        val directions = ProbeOctoPrintFragmentDirections.requestAccess(webUrl)
        findNavController().navigate(directions, extras)
    }

    private fun continueWithPresentApiKey(finding: TestFullNetworkStackUseCase.Finding.OctoPrintReady) {
        // If the "thing" which started this screen allowed us to reuse an existing API key OR the user has the quick switch feature, we can continue with the existing
        // API key. Otherwise, the user is forced to reconnect OctoPrint. Reusing might be explicitly allowed, when this fragment is started to troubleshoot the connection.
        if (allowApiKeyReuse || BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH)) {
            val repo = Injector.get().octorPrintRepository()
            val oldInstance = repo.findOrNull(finding.webUrl)
            val instance = oldInstance?.copy(
                webUrl = finding.webUrl,
                apiKey = finding.apiKey
            ) ?: OctoPrintInstanceInformationV2(
                webUrl = finding.webUrl,
                apiKey = finding.apiKey
            )

            Injector.get().octorPrintRepository().setActive(instance)
        }
    }

    private fun showLoading() {
        beginDelayedTransition()
        binding.octoView.scheduleAnimation(600) { swim() }
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
        setUpAsHelpButton(b.help)

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
        is TestFullNetworkStackUseCase.Finding.ServerIsNotOctoPrint -> "**${finding.host}** might not be an OctoPrint"
        is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.WebSocketUpgradeFailed -> "HTTP access to **${finding.host}** works, but web socket broken"
    }

    private fun getExplainerForFinding(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> "Please enter your Basic Auth credentials below, not your OctoPrint user and password."
        is TestFullNetworkStackUseCase.Finding.DnsFailure -> "Android is not able to resolve the IP address for **${finding.host}**. Check following things to resolve the issue:\n\n- Ensure there is no typo\n- Make sure you are connected to the internet\n- Make sure you are on the correct WiFi network\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser\n- Try using the **IP address** instead of **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.LocalDnsFailure -> "Android is not able to resolve the IP address for **${finding.host}**. Android devices are sometimes configured by the manufacturer in a way that they cannot reliably resolve .home or .local domains. Check following things to resolve the issue:\n\n- Ensure there is no typo\n- Make sure you are on the correct WiFi network\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser\n- Try using the **IP address** instead of **${finding.host}**"
        is TestFullNetworkStackUseCase.Finding.HostNotReachable -> "OctoApp resolved the IP of **${finding.host}**, but **${finding.ip}** can't be reached within 2 seconds. Check following things to resolve the issue:\n\n- Make sure the machine hosting OctoPrint is turned on and connected\n- If your OctoPrint is only available locally, make sure you are connected to the correct WiFi network\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser"
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> if (finding.weakHostnameVerificationRequired) {
            "The SSL certificate presented by your OctoPrint can not be used to establish a HTTPS connection because Android does not trust the certificate. This is a common issue for self-signed certificates that were not installed on this phone.\n\nAdditionally, the certificate uses a deprecated format that does not comply to modern security standards. A \"subject alternative name\" (SAN) field is required from Android 9 onwards. \n\nOctoApp can add this certificate to a trusted list and will bypass Android’s check for this certificate."
        } else {
            "The SSL certificate presented by your OctoPrint can not be used to establish a HTTPS connection because Android does not trust the certificate. This is a common issue for self-signed certificates that were not installed on this phone.\n\nOctoApp can add this certificate to a trusted list and will bypass Android’s check for this certificate."
        }
        is TestFullNetworkStackUseCase.Finding.InvalidUrl -> "The URL **${finding.webUrl}** seems to contain a syntax error and can't be parsed. Android reports following error:\n\n**${finding.exception.localizedMessage ?: "Unknown error"}**"
        is TestFullNetworkStackUseCase.Finding.OctoPrintNotFound -> "OctoApp was able to connect to **${finding.webUrl}** but received a 404 response. Check following things to resolve the issue:\n\n- Make sure you use the correct port and host\n- Make sure that you use the correct path in the URL\n- Try to open [**${finding.webUrl}](**${finding.webUrl}**) in your browser"
        is TestFullNetworkStackUseCase.Finding.PortClosed -> "OctoApp was able to connect to **${finding.host}** but port **${finding.port}** is closed. Check following things to resolve the issue:\n\n- Make sure **${finding.port}** is correct. If you don't specify the port explicitly, OctoApp will use 80 for HTTP and 443 for HTTPS\n- Try to open [${finding.webUrl}](${finding.webUrl}) in your browser"
        is TestFullNetworkStackUseCase.Finding.UnexpectedHttpIssue -> "OctoApp was able to communicate with **${finding.host}**, but when trying to establish a HTTP(S) connection an unexpected error occurred. Android reports following issue:\n\n**${finding.exception.localizedMessage ?: "Unknown error"}**\n\nYou can **long-press \"Need help?\"** above to get support (please include logs)."
        is TestFullNetworkStackUseCase.Finding.UnexpectedIssue -> "OctoApp encountered an unexpected error. Android reports following issue:\n\n**${finding.exception.localizedMessage ?: "Unknown error"}**\n\nYou can **long-press \"Need help?\"** above to get support (please include logs)."
        is TestFullNetworkStackUseCase.Finding.ServerIsNotOctoPrint -> "OctoApp was able to communicate with **${finding.host}**, but the server seems not to be a recent version of OctoPrint.\n\nYou can continue, but other issues may arise in the following steps."
        is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> "" // Never shown
        is TestFullNetworkStackUseCase.Finding.WebSocketUpgradeFailed -> "OctoApp can communicate with **${finding.host}**, but the web socket failed to connect with response code **${finding.responseCode}**. This is a very common issue with incorrectly configured reverse proxy setups. Check following things to resolve the issue:\n\n- Make sure your proxy sets the `Upgrade: WebSocket` header for `${finding.webSocketUrl}` as it is not forwarded to OctoPrint by default\n- Refer to the [configuration examples in the OctoPrint community](https://community.octoprint.org/t/reverse-proxy-configuration-examples/1107) for Nginx, HAProxy, Apache and others"
    }

    private fun getPrimaryActionText(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> "Trust & Continue"
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> "Continue"
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
            viewModel.probe(finding.webUrl)
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
            val webUrl = uri.buildUpon().encodedAuthority("$credentials${uri.host}").build().toString()
            Injector.get().sensitiveDataMask().registerWebUrl(webUrl, "octoprint")
            viewModel.probe(webUrl)
        }

        else -> viewModel.probe(finding.webUrl)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }
}