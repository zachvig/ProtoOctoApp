package de.crysxd.octoapp.signin.usecases

import android.content.Context
import android.util.Patterns
import androidx.core.text.isDigitsOnly
import de.crysxd.octoapp.base.UseCase
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult

class VerifySignInInformationUseCase(private val context: Context) :
    UseCase<SignInInformation, SignInInformationValidationResult> {

    override suspend fun execute(param: SignInInformation): SignInInformationValidationResult {
        val ip = verifyIp(param.ipAddress)
        val port = verifyPort(param.port)
        val apiKey = verifyApiKey(param.apiKey)

        return if (listOf(ip, port, apiKey).all { it == null }) {
            SignInInformationValidationResult.ValidationOk
        } else {
            SignInInformationValidationResult.ValidationFailed(ip, port, apiKey)
        }
    }

    private fun verifyIp(string: CharSequence) =
        verify(string, context.getString(R.string.a_ip_address)) ?: when {
            !Patterns.IP_ADDRESS.matcher(string)
                .matches() -> context.getString(R.string.enter_a_valid_ip_address)
            else -> null
        }

    private fun verifyPort(string: CharSequence) =
        verify(string, context.getString(R.string.the_port)) ?: when {
            !string.isDigitsOnly() -> context.getString(R.string.only_digits)
            else -> null
        }

    private fun verifyApiKey(string: CharSequence) =
        verify(string, context.getString(R.string.an_api_key))

    private fun verify(string: CharSequence, thing: String) = when {
        string.isBlank() -> context.getString(R.string.please_enter_x, thing)
        else -> null
    }

}