package de.crysxd.octoapp.ui.signin

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputLayout
import de.crysxd.models.SignInInformation
import de.crysxd.models.SignInInformationValidationResult
import de.crysxd.octoapp.R
import de.crysxd.octoapp.di.injectViewModel
import de.crysxd.octoapp.ui.BaseFragment
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
            SignInInformation(
                textInputLayoutIpAddress.editText?.text ?: "",
                textInputLayoutPort.editText?.text ?: "",
                textInputLayoutApiKey.editText?.text ?: ""
            )
        )
    }
}