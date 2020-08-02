package de.crysxd.octoapp.signin.usecases

import android.content.Context
import android.net.Uri
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
        require(string.all { it.isLetterOrDigit() })
        require(string.startsWith("http://") || string.startsWith("https://"))
        requireNotNull(Uri.parse(string.toString()))
        null
    } catch (e: Exception) {
        context.getString(R.string.enter_a_valid_web_url)
    }

    private fun verifyApiKey(string: CharSequence) = when {
        string.isBlank() -> context.getString(R.string.error_enter_api_key)
        string.toString().any { !it.isLetterOrDigit() } -> context.getString(R.string.error_api_cotains_illegal_characters)
        else -> null
    }

}