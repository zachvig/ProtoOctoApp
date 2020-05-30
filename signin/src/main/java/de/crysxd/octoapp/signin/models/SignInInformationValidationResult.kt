package de.crysxd.octoapp.signin.models

sealed class SignInInformationValidationResult {

    object ValidationOk : SignInInformationValidationResult()
    data class ValidationFailed(
        val ipErrorMessage: String?,
        val portErrorMessage: String?,
        val apiKeyErrorMessage: String?
    ) : SignInInformationValidationResult()

}