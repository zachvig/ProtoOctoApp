package de.crysxd.octoapp.signin.usecases

import android.content.Context
import android.net.Uri
import androidx.core.text.isDigitsOnly
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult

class VerifySignInInformationUseCase(private val context: Context) :
    UseCase<SignInInformation, SignInInformationValidationResult> {

    override suspend fun execute(param: SignInInformation): SignInInformationValidationResult {
        val webUrl = verifyWebUrl(param.webUrl)
        val apiKey = verifyApiKey(param.apiKey)

        return if (listOf(webUrl, apiKey).all { it == null }) {
            SignInInformationValidationResult.ValidationOk
        } else {
            SignInInformationValidationResult.ValidationFailed(webUrl, apiKey)
        }
    }

    private fun verifyWebUrl(string: CharSequence) = try {
        requireNotNull(Uri.parse(string.toString()))
        null
    } catch (e: Exception) {
        context.getString(R.string.enter_a_valid_ip_address)
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