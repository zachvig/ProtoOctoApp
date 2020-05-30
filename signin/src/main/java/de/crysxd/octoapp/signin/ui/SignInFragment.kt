package de.crysxd.octoapp.signin.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputLayout
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult
import kotlinx.android.synthetic.main.fragment_signin.*


class SignInFragment : BaseFragment(R.layout.fragment_signin) {

   override val viewModel: SignInViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSignIn.setOnClickListener(this::signIn)
        textInputLayoutApiKey.editText?.let {
            it.setImeActionLabel(getString(R.string.sign_in), KeyEvent.KEYCODE_ENTER)
            it.setOnEditorActionListener { v, _, _ ->
                signIn(v)
                true
            }
        }
        viewModel.verificationStatus.observe(this, Observer(this::updateValidationResult))
    }

    private fun updateValidationResult(res: SignInInformationValidationResult) {
        fun applyError(til: TextInputLayout, error: String?) {
            til.error = error
            til.isErrorEnabled = error != null
        }

        applyError(
            textInputLayoutIpAddress,
            (res as? SignInInformationValidationResult.ValidationFailed)?.ipErrorMessage
        )
        applyError(
            textInputLayoutPort,
            (res as? SignInInformationValidationResult.ValidationFailed)?.portErrorMessage
        )
        applyError(
            textInputLayoutApiKey,
            (res as? SignInInformationValidationResult.ValidationFailed)?.apiKeyErrorMessage
        )
    }

    private fun signIn(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModel.startSignIn(
            de.crysxd.octoapp.signin.models.SignInInformation(
                textInputLayoutIpAddress.editText?.text ?: "",
                textInputLayoutPort.editText?.text ?: "",
                textInputLayoutApiKey.editText?.text ?: ""
            )
        )
    }
}