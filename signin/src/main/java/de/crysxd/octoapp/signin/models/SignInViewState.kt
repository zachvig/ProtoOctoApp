package de.crysxd.octoapp.signin.models

sealed class SignInViewState {
    object Idle : SignInViewState()
    object Loading: SignInViewState()
    data class SignInInformationInvalid(val result: SignInInformationValidationResult.ValidationFailed) : SignInViewState()
    object SignInFailed : SignInViewState()
    object SignInSuccess : SignInViewState()
}