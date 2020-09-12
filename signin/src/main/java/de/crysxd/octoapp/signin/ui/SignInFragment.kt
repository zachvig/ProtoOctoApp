package de.crysxd.octoapp.signin.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.ext.setTextAppearanceCompat
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.utils.ViewCompactor
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInViewState
import de.crysxd.octoapp.signin.usecases.SignInUseCase.Warning.NotAdmin
import de.crysxd.octoapp.signin.usecases.SignInUseCase.Warning.TooNewServerVersion
import kotlinx.android.synthetic.main.fragment_signin.*
import timber.log.Timber

class SignInFragment : BaseFragment(R.layout.fragment_signin) {

    override val viewModel: SignInViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSignIn.setOnClickListener(this::signIn)
        inputApiKey.editText.setImeActionLabel(getString(R.string.sign_in), KeyEvent.KEYCODE_ENTER)
        inputApiKey.editText.setOnEditorActionListener { v, _, _ ->
            signIn(v)
            true
        }

        viewModel.viewState.observe(viewLifecycleOwner, Observer(this::updateViewState))

        val preFill = viewModel.getPreFillInfo()
        inputWebUrl.editText.setText(preFill.webUrl)
        inputApiKey.editText.setText(preFill.apiKey)

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(ReadQrCodeFragment.RESULT_API_KEY)?.observe(viewLifecycleOwner, Observer {
            inputApiKey.editText.setText(it)
        })

        inputApiKey.setOnActionListener {
            Firebase.analytics.logEvent("qr_code_login", Bundle.EMPTY)
            findNavController().navigate(R.id.actionReadQrCode)
        }

        val full = ConstraintSet().also { it.load(requireContext(), R.layout.fragment_signin) }
        val compact = ConstraintSet().also { it.load(requireContext(), R.layout.fragment_signin_compact) }

        ViewCompactor(view as ViewGroup, reset = {
            Timber.i("Reset")
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup, InstantAutoTransition(quickTransition = true, explode = true))
            full.applyTo(constraintLayout)
            textViewTitle.setTextAppearanceCompat(R.style.OctoTheme_TextAppearance_Title_Large)
        }, compact = {
            Timber.i("Compact $it")
            compact.applyTo(constraintLayout)
            textViewTitle.setTextAppearanceCompat(R.style.OctoTheme_TextAppearance_Title)
            false
        })
    }

    private fun updateViewState(res: SignInViewState) {
        if (res is SignInViewState.SignInFailed) {
            AlertDialog.Builder(requireContext())
                .setMessage(res.exception.composeMessage())
                .setPositiveButton(android.R.string.ok, null)
                .also {
                    if (res.failedAttempts >= 3) {
                        it.setNeutralButton(R.string.get_support) { _, _ ->
                            SendFeedbackDialog().show(childFragmentManager, "sign-in-support")
                        }
                    }
                }
                .setNegativeButton(R.string.show_details) { _, _ ->
                    AlertDialog.Builder(requireContext())
                        .setMessage(res.exception.composeMessageStack())
                        .setPositiveButton(android.R.string.ok, null)
                        .setNeutralButton(R.string.get_support) { _, _ ->
                            SendFeedbackDialog().show(childFragmentManager, "sign-in-support")
                        }
                        .show()
                }
                .show()
        }

        if (res is SignInViewState.SignInInformationInvalid) {
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

            // Show warning dialog if
            if (res.warnings.isNotEmpty()) {
                val message = res.warnings.joinToString("\n") {
                    val text = when (it) {
                        is TooNewServerVersion -> getString(R.string.warning_server_version_too_new, it.testedOnVersion, it.serverVersion)
                        is NotAdmin -> getString(R.string.warning_no_admin_rights)
                    }

                    "⚠️ $text"
                }

                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.sign_in_succes_with_warnings))
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()

            }

            viewModel.completeSignIn(res.instanceInformation)
        }
    }

    private fun signIn(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModel.startSignIn(
            SignInInformation(
                inputWebUrl.editText.text.toString(),
                inputApiKey.editText.text.toString()
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
    }

    private fun Exception.composeMessage(): CharSequence = HtmlCompat.fromHtml(
        getString(
            R.string.error_unable_to_connect,
            ContextCompat.getColor(requireContext(), R.color.light_text),
            localizedMessage ?: cause?.localizedMessage ?: this::class.java.simpleName
        ), HtmlCompat.FROM_HTML_MODE_LEGACY
    )

    private fun Exception.composeMessageStack(): CharSequence {
        val messageBuilder = StringBuilder()
        messageBuilder.append("<b>Error:</b> ")
        messageBuilder.append(localizedMessage ?: this::class.java.simpleName)

        var cause = cause
        while (cause != null) {
            messageBuilder.append("<br/><br/>")
            messageBuilder.append("<b>Caused by:</b> ")
            messageBuilder.append(cause.localizedMessage)
            cause = cause.cause
        }

        return HtmlCompat.fromHtml(messageBuilder.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}