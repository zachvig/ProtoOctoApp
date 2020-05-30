package de.crysxd.octoapp.ui.signin

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputLayout
import de.crysxd.octoapp.R
import kotlinx.android.synthetic.main.fragment_signin.*

class SignInFragment : Fragment(R.layout.fragment_signin) {

    private val viewModel: SignInViewModel by viewModels()

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
        viewModel.verificationStatus.observe(this, Observer(this::updateVerificationResult))
    }

    private fun updateVerificationResult(res: SignInViewModel.VerificationResult) {
        fun applyError(til: TextInputLayout, error: String?) {
            til.error = error
            til.isErrorEnabled = error != null
        }

        applyError(textInputLayoutIpAddress, res.ipErrorMessage)
        applyError(textInputLayoutPort, res.portErrorMessage)
        applyError(textInputLayoutApiKey, res.apiKeyErrorMessage)
    }

    private fun signIn(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModel.startSignIn(
            textInputLayoutIpAddress.editText?.text ?: "",
            textInputLayoutPort.editText?.text ?: "",
            textInputLayoutApiKey.editText?.text ?: ""
        )
    }
}