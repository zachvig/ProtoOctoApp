package de.crysxd.octoapp.ui.signin

import android.util.Patterns
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import de.crysxd.octoapp.R
import de.crysxd.octoapp.di.Injector

class SignInViewModel : ViewModel() {

    private val context = Injector.get().context()
    private val mutableVerificationStatus = MutableLiveData<VerificationResult>()
    val verificationStatus = Transformations.map(mutableVerificationStatus) { it }

    fun startSignIn(
        ip: CharSequence,
        port: CharSequence,
        apiKey: CharSequence
    ) {
        val res = VerificationResult(
            verifyIp(ip),
            verifyPort(port),
            verifyApiKey(apiKey)
        )

        mutableVerificationStatus.postValue(res)

        if (res.allOk()) {
            TODO("Sign in")
        }
    }

    private fun verifyIp(string: CharSequence) = verify(string, context.getString(R.string.a_ip_address)) ?: when {
        !Patterns.IP_ADDRESS.matcher(string).matches() -> context.getString(R.string.enter_a_valid_ip_address)
        else -> null
    }

    private fun verifyPort(string: CharSequence) = verify(string, context.getString(R.string.the_port)) ?: when {
        !string.isDigitsOnly() -> context.getString(R.string.only_digits)
        else -> null
    }

    private fun verifyApiKey(string: CharSequence) = verify(string, context.getString(R.string.an_api_key))

    private fun verify(string: CharSequence, thing: String) = when {
        string.isBlank() -> context.getString(R.string.please_enter_x, thing)
        else -> null
    }

    data class VerificationResult(
        val ipErrorMessage: String?,
        val portErrorMessage: String?,
        val apiKeyErrorMessage: String?
    ) {

        fun allOk() =
            ipErrorMessage == null && portErrorMessage == null && apiKeyErrorMessage == null

    }
}