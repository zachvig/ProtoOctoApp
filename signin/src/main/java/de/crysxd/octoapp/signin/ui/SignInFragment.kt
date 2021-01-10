package de.crysxd.octoapp.signin.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.composeErrorMessage
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.ext.setTextAppearanceCompat
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.utils.ViewCompactor
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInViewState
import de.crysxd.octoapp.signin.troubleshoot.TroubleShootingFragmentArgs
import de.crysxd.octoapp.signin.usecases.SignInUseCase.Warning.TooNewServerVersion
import kotlinx.android.synthetic.main.fragment_signin.*
import timber.log.Timber
import java.net.URL
import java.security.cert.Certificate

class SignInFragment : BaseFragment(R.layout.fragment_signin), InsetAwareScreen {

    override val viewModel: SignInViewModel by injectViewModel()
    private val networkViewModel: NetworkStateViewModel by injectViewModel(Injector.get().viewModelFactory())
    private var viewCompactor: ViewCompactor? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var noWifiLogged = false
        networkViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Network status: $it")
            if (!noWifiLogged && it !is NetworkStateViewModel.NetworkState.WifiConnected) {
                noWifiLogged = true
                OctoAnalytics.logEvent(OctoAnalytics.Event.SignInNoWifiWarningShown)
            }
            inputWebUrl.actionTint = null
            inputWebUrl.actionIcon = if (it !is NetworkStateViewModel.NetworkState.WifiConnected) R.drawable.ic_wifi_unavailable else 0
            inputWebUrl.setOnActionListener {
                OctoAnalytics.logEvent(OctoAnalytics.Event.SignInNoWifiWarningTapped)
                requireOctoActivity().showDialog(getString(R.string.no_wifi_warning_long))
            }
        }

        buttonSignIn.setOnClickListener { signIn() }
        inputApiKey.editText.setImeActionLabel(getString(R.string.sign_in), KeyEvent.KEYCODE_ENTER)
        inputApiKey.editText.setOnEditorActionListener { _, _, _ ->
            signIn()
            true
        }

        manual.text = HtmlCompat.fromHtml("<a href=\"\">${getString(R.string.sign_in_manual_link)}</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        manual.setOnClickListener {
            val uri = Uri.parse(Firebase.remoteConfig.getString("help_url_sign_in"))
            OctoAnalytics.logEvent(OctoAnalytics.Event.SignInHelpOpened)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        manual.movementMethod = LinkMovementMethod()

        viewModel.viewState.observe(viewLifecycleOwner, Observer(this::updateViewState))

        val preFill = viewModel.getPreFillInfo()
        inputWebUrl.editText.setText(preFill.webUrl)
        inputApiKey.editText.setText(preFill.apiKey)

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(ReadQrCodeFragment.RESULT_API_KEY)?.observe(viewLifecycleOwner, {
            inputApiKey.editText.setText(it)
        })

        inputApiKey.setOnActionListener {
            OctoAnalytics.logEvent(OctoAnalytics.Event.QrCodeStarted)
            findNavController().navigate(R.id.actionReadQrCode)
        }

        val full = ConstraintSet().also { it.load(requireContext(), R.layout.fragment_signin) }
        val compact0 = ConstraintSet().also { it.load(requireContext(), R.layout.fragment_signin) }
        val compact1 = ConstraintSet().also { it.load(requireContext(), R.layout.fragment_signin) }
        val compact2 = ConstraintSet().also { it.load(requireContext(), R.layout.fragment_signin_compact) }
        compact0.setMargin(R.id.octoView, ConstraintSet.TOP, requireContext().resources.getDimension(R.dimen.margin_5).toInt())
        compact1.setMargin(R.id.octoView, ConstraintSet.TOP, requireContext().resources.getDimension(R.dimen.margin_4).toInt())

        viewCompactor = ViewCompactor(view as ViewGroup, reset = {
            Timber.i("Reset")
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup, InstantAutoTransition(quickTransition = true, explode = true))
            full.applyTo(constraintLayout)
            textViewTitle.setTextAppearanceCompat(R.style.OctoTheme_TextAppearance_Title_Large)
        }, compact = {
            Timber.i("Compact $it")

            when (it) {
                0 -> {
                    compact0.applyTo(constraintLayout)
                    true
                }
                1 -> {
                    compact1.applyTo(constraintLayout)
                    true
                }
                2 -> {
                    compact2.applyTo(constraintLayout)
                    textViewTitle.setTextAppearanceCompat(R.style.OctoTheme_TextAppearance_Title)
                    false
                }
                else -> false
            }
        })
    }

    private fun updateViewState(res: SignInViewState) {
        if (res is SignInViewState.SignInFailed && !res.shownToUser) {
            res.shownToUser = true
            OctoAnalytics.logEvent(
                OctoAnalytics.Event.SignInFailed,
                bundleOf(
                    "reason_messsage" to res.exception.message,
                    "reason" to res.exception::class.java.simpleName
                )
            )

            if (res.exception is BasicAuthRequiredException) {
                res.exception.enrichUserMessageWithUrl(URL(res.baseUrl.toString()))
            }

            // SSL error and we can "force trust" this server if user agrees
            if (res.exception is OctoPrintHttpsException && res.exception.serverCertificates.isNotEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.error_certificate_not_trusted)
                    .setPositiveButton(R.string.trust_server_and_try_again) { _, _ ->
                        signIn(res.exception.serverCertificates)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            // Any other error
            else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(res.exception.composeErrorMessage(requireContext(), R.string.error_unable_to_connect))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.trouble_shooting) { _, _ ->
                        OctoAnalytics.logEvent(OctoAnalytics.Event.TroubleShootFromSignIn)
                        inputWebUrl.editText.clearFocusAndHideSoftKeyboard()
                        inputApiKey.editText.clearFocusAndHideSoftKeyboard()
                        findNavController().navigate(
                            R.id.actionTroubleShoot,
                            TroubleShootingFragmentArgs(
                                baseUrl = res.baseUrl,
                                apiKey = res.apiKey
                            ).toBundle()
                        )
                    }
                    .show()
            }
        }

        if (res is SignInViewState.SignInInformationInvalid) {
            OctoAnalytics.logEvent(
                OctoAnalytics.Event.SignInInvalidInput,
                bundleOf(
                    "api_key_invalid" to (res.result.apiKeyErrorMessage != null),
                    "web_url_invalid" to (res.result.webUrlErrorMessage != null),
                    "api_key_error" to res.result.apiKeyErrorMessage,
                    "web_url_error" to res.result.webUrlErrorMessage,
                    "web_url_input" to inputWebUrl.editText.text.toString(),
                )
            )
            inputWebUrl.error = res.result.webUrlErrorMessage
            inputApiKey.error = res.result.apiKeyErrorMessage
        } else {
            inputWebUrl.error = null
            inputApiKey.error = null
        }

        if (res is SignInViewState.Loading) {
            buttonSignIn.isEnabled = false
            buttonSignIn.text = getString(R.string.loading)
        } else {
            buttonSignIn.isEnabled = true
            buttonSignIn.text = getString(R.string.sign_in_to_octoprint)
        }

        @Suppress("ControlFlowWithEmptyBody")
        if (res is SignInViewState.SignInSuccess) {
            buttonSignIn.isEnabled = false
            OctoAnalytics.logEvent(OctoAnalytics.Event.SignInSuccess)

            // Show warning dialog if
            if (res.warnings.isNotEmpty()) {
                val message = res.warnings.joinToString("\n") {
                    val text = when (it) {
                        is TooNewServerVersion -> getString(R.string.warning_server_version_too_new, it.testedOnVersion, it.serverVersion)
                    }

                    "⚠️ $text"
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.sign_in_succes_with_warnings))
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

            }

            viewModel.completeSignIn(res.instanceInformation)
        }
    }

    private fun signIn(trustedCerts: List<Certificate>? = null) {
        viewModel.startSignIn(
            SignInInformation(
                webUrl = inputWebUrl.editText.text.toString(),
                apiKey = inputApiKey.editText.text.toString(),
                trustedCerts = trustedCerts
            )
        )
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false

        // Show message if logout because of invalid API key
        // Do not show if already shown in this "session"
        if (viewModel.getPreFillInfo().apiKeyWasInvalid && !viewModel.invalidApiKeyInfoWasShown) {
            viewModel.invalidApiKeyInfoWasShown = true
            requireOctoActivity().showDialog(requireContext().getString(R.string.error_api_key_reported_invalid))
        }

        val knownSignInInfo = viewModel.getKnownSignInInfo()
        val labels = knownSignInInfo.map { info -> info.webUrl }.toTypedArray()
        buttonMore.isVisible = knownSignInInfo.isNotEmpty()
        buttonMore.setOnClickListener {
            MaterialAlertDialogBuilder(it.context)
                .setItems(labels) { _, position ->
                    val info = knownSignInInfo[position]
                    inputWebUrl.editText.setText(info.webUrl)
                    inputApiKey.editText.setText(info.apiKey)
                    viewModel.completeSignIn(info)
                }
                .setTitle("Reconnect to:")
                .show()
        }
    }

    override fun handleInsets(insets: Rect) {
        requireView().updatePadding(
            top = insets.top,
            left = insets.left,
            right = insets.right,
            bottom = insets.bottom,
        )
        viewCompactor?.notifyChanged()
    }
}