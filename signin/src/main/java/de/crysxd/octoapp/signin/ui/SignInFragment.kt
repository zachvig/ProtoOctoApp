package de.crysxd.octoapp.signin.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputLayout
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult
import de.crysxd.octoapp.signin.models.SignInViewState
import kotlinx.android.synthetic.main.fragment_signin.*
import de.crysxd.octoapp.base.R as BaseR

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
        viewModel.viewState.observe(this, Observer(this::updateViewState))
    }

    private fun showError(til: TextInputLayout, error: String?) {
        til.error = error
        til.isErrorEnabled = error != null
    }

    private fun updateViewState(res: SignInViewState) {
        if (res is SignInViewState.SignInFailed) {
            AlertDialog.Builder(requireContext())
                .setMessage("Unable to connect to OctoPrint with the provided information.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        if (res is SignInViewState.SignInInformationInvalid) {
            showError(textInputLayoutIpAddress, res.result.ipErrorMessage)
            showError(textInputLayoutPort, res.result.portErrorMessage)
            showError(textInputLayoutApiKey, res.result.apiKeyErrorMessage)
        } else {
            showError(textInputLayoutIpAddress, null)
            showError(textInputLayoutPort, null)
            showError(textInputLayoutApiKey, null)
        }

        if (res is SignInViewState.Loading) {
            buttonSignIn.isEnabled = false
            buttonSignIn.text = getString(R.string.loading)
        } else {
            buttonSignIn.isEnabled = true
            buttonSignIn.text = getString(R.string.sign_in)
        }

        if (res is SignInViewState.SignInSuccess) {
            requireView().findNavController().navigate(BaseR.id.action_sign_in_completed)
        }
    }

    private fun signIn(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModel.startSignIn(
            SignInInformation(
                textInputLayoutIpAddress.editText?.text?.toString() ?: "",
                textInputLayoutPort.editText?.text?.toString() ?: "",
                textInputLayoutApiKey.editText?.text?.toString() ?: ""
            )
        )
    }
}