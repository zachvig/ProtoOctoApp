package de.crysxd.octoapp.signin.usecases

import android.content.Context
import android.net.Uri
import de.crysxd.octoapp.base.usecase.UseCase
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult
import timber.log.Timber

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
        require(string.all { it.isLetterOrDigit() }) { "Not all characters valid: $string" }
        require(string.startsWith("http://") || string.startsWith("https://")) { "Not starting with HTTP(s): $string" }
        requireNotNull(Uri.parse(string.toString())) { "Uri is null: $string" }
        null
    } catch (e: Exception) {
        Timber.i("Validation error: ${e.message}")
        context.getString(R.string.enter_a_valid_web_url)
    }

    private fun verifyApiKey(string: CharSequence) = when {
        string.isBlank() -> context.getString(R.string.error_enter_api_key)
        string.toString().any { !it.isLetterOrDigit() } -> context.getString(R.string.error_api_cotains_illegal_characters)
        else -> null
    }

}