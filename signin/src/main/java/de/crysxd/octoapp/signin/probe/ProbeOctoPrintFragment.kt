package de.crysxd.octoapp.signin.probe

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
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
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
import okhttp3.HttpUrl
import timber.log.Timber
import java.util.UUID

class ProbeOctoPrintFragment : BaseFragment() {

    override val viewModel by injectViewModel<ProbeOctoPrintViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(Injector.get().viewModelFactory())
    private var loadingBinding: ProbeFragmentInitialBinding? = null
    private var findingBinding: ProbeFragmentFindingBinding? = null
    private val findingDescriptionLibrary by lazy { FindingDescriptionLibrary(requireContext()) }
    private val initialWebUrl by lazy {
        UriLibrary.secureDecode(navArgs<ProbeOctoPrintFragmentArgs>().value.baseUrl)
    }
    private val instanceId by lazy {
        navArgs<ProbeOctoPrintFragmentArgs>().value.instanceId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
        viewModel.probe(initialWebUrl)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loading.subtitle.isVisible = false
        binding.loading.title.text = getString(R.string.sign_in___probe___probing_active_title)

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                ProbeOctoPrintViewModel.UiState.Loading -> showLoading()
                is ProbeOctoPrintViewModel.UiState.FindingsReady -> {
                    when (it.finding) {
                        is TestFullNetworkStackUseCase.Finding.OctoPrintReady,
                        is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> {
                            // We might get navigated back to this fragment in case there are issues later down the road
                            // This checks makes sure we are never reusing a old result to navigate away and instead restarting the probe if the screen
                            // gets shown again
                            if (it.handled) {
                                viewModel.probe(viewModel.lastWebUrl?.toString() ?: initialWebUrl)
                                return@observe
                            }

                            it.handled = true
                            when (it.finding) {
                                is TestFullNetworkStackUseCase.Finding.OctoPrintReady -> continueWithPresentApiKey(it.finding)
                                is TestFullNetworkStackUseCase.Finding.InvalidApiKey -> continueToRequestApiKey(it.finding.webUrl)
                                else -> Unit
                            }
                        }

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

    private fun continueToRequestApiKey(webUrl: HttpUrl) = binding.octoView.doAfterAnimation {
        // Octo is now in a neutral position, we can animate states
        val extras = FragmentNavigatorExtras(binding.octoView to "octoView", binding.octoBackground to "octoBackground")
        val directions = ProbeOctoPrintFragmentDirections.requestAccess(UriLibrary.secureEncode(webUrl.toString()))
        findNavController().navigate(directions, extras)
    }

    private fun continueWithPresentApiKey(finding: TestFullNetworkStackUseCase.Finding.OctoPrintReady) {
        // If the "thing" which started this screen gave us a instance id of an existing instance OR the user has the quick switch feature, we can continue with the existing
        // API key. Otherwise, the user is forced to reconnect OctoPrint. Reusing might be explicitly allowed, when this fragment is started to troubleshoot the connection.
        if (instanceId != null || BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH)) {
            val repo = Injector.get().octorPrintRepository()
            val oldInstance = instanceId?.let { repo.get(it) }
            val instance = oldInstance?.copy(
                webUrl = finding.webUrl,
                apiKey = finding.apiKey
            ) ?: OctoPrintInstanceInformationV3(
                id = UUID.randomUUID().toString(),
                webUrl = finding.webUrl,
                apiKey = finding.apiKey
            )

            // Clearing and setting the instance will ensure we reset the navigation
            Injector.get().octorPrintRepository().clearActive()
            Injector.get().octorPrintRepository().setActive(instance.copy(issue = null))
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

        markwon.setMarkdown(b.content, findingDescriptionLibrary.getExplainerForFinding(finding))
        markwon.setMarkdown(b.title, findingDescriptionLibrary.getTitleForFinding(finding))

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
        if (isInTestOnlyMode()) {
            // Case A: We got here because a API key was invalid, there is an active instance
            goBackToDiscover()
        } else {
            // Case B: User is signing in, but nothing is active yet
            findNavController().popBackStack()
        }
    }

    private fun getEditButtonText() = if (isInTestOnlyMode()) {
        // Case A: We got here because a API key was invalid, there is an active instance
        getString(R.string.sign_in___connect_to_other_octoprint)
    } else {
        // Case B: User is signing in, but nothing is active yet
        getString(R.string.sign_in___probe___edit_information)
    }

    private fun isInTestOnlyMode() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot() != null

    private fun getPrimaryActionText(finding: TestFullNetworkStackUseCase.Finding) = when (finding) {
        is TestFullNetworkStackUseCase.Finding.HttpsNotTrusted -> getString(R.string.sing_in___probe___trust_and_continue)
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> getString(R.string.sign_in___continue)
        else -> getString(R.string.sign_in___try_again)
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
            viewModel.probe(finding.webUrl.toString())
        }
        is TestFullNetworkStackUseCase.Finding.BasicAuthRequired -> {
            val webUrl = finding.webUrl.newBuilder()
                .username(findingBinding?.usernameInput?.editText?.text?.toString() ?: "")
                .password(findingBinding?.passwordInput?.editText?.text?.toString() ?: "")
                .build()
            Injector.get().sensitiveDataMask().registerWebUrl(webUrl)
            viewModel.probe(webUrl.toString())
        }

        else -> viewModel.probe(finding.webUrl.toString())
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }
}