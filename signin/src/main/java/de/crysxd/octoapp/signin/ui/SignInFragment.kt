package de.crysxd.octoapp.signin.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.ext.setTextAppearanceCompat
import de.crysxd.octoapp.base.ui.utils.ViewCompactor
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInViewState
import kotlinx.android.synthetic.main.fragment_signin.*

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

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(ReadQrCodeFragment.RESULT_API_KEY)?.observe(viewLifecycleOwner, Observer {
            inputApiKey.editText.setText(it)
        })

        inputApiKey.setOnActionListener {
            findNavController().navigate(R.id.actionReadQrCode)
        }

        val full = ConstraintSet()
        full.load(requireContext(), R.layout.fragment_signin)
        val compact = ConstraintSet()
        compact.load(requireContext(), R.layout.fragment_signin_compact)
        ViewCompactor(view as ViewGroup, reset = {
            full.applyTo(constraintLayout)
            textViewTitle.setTextAppearanceCompat(R.style.OctoTheme_TextAppearance_Title_Large)
            textViewTitle.gravity = Gravity.CENTER
            textViewSubTitle.gravity = Gravity.CENTER
        }, compact = {
            compact.applyTo(constraintLayout)
            textViewTitle.setTextAppearanceCompat(R.style.OctoTheme_TextAppearance_Title)
            textViewTitle.gravity = Gravity.START
            textViewSubTitle.gravity = Gravity.START
            false
        })
    }

    private fun updateViewState(res: SignInViewState) {
        if (res is SignInViewState.SignInFailed) {
            AlertDialog.Builder(requireContext())
                .setMessage("Unable to connect to OctoPrint with the provided information.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        if (res is SignInViewState.SignInInformationInvalid) {
            inputHostname.setError(res.result.ipErrorMessage)
            inputPort.setError(res.result.portErrorMessage)
            inputApiKey.setError(res.result.apiKeyErrorMessage)
        } else {
            inputHostname.setError(null)
            inputPort.setError(null)
            inputApiKey.setError(null)
        }

        if (res is SignInViewState.Loading) {
            buttonSignIn.isEnabled = false
            buttonSignIn.text = getString(R.string.loading)
        } else {
            buttonSignIn.isEnabled = true
            buttonSignIn.text = getString(R.string.sign_in)
        }

        @Suppress("ControlFlowWithEmptyBody")
        if (res is SignInViewState.SignInSuccess) {
            // MainActivity will navigate away
        }
    }

    private fun signIn(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModel.startSignIn(
            SignInInformation(
                inputHostname.editText.text.toString(),
                inputPort.editText.text.toString(),
                inputApiKey.editText.text.toString()
            )
        )
    }
}