package de.crysxd.octoapp.signin.models

sealed class SignInInformationValidationResult {

    object ValidationOk : SignInInformationValidationResult()
    data class ValidationFailed(
        val webUrlErrorMessage: String?,
        val apiKeyErrorMessage: String?
    ) : SignInInformationValidationResult()

}