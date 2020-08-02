package de.crysxd.octoapp.signin.models

import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.signin.usecases.SignInUseCase

sealed class SignInViewState {
    object Idle : SignInViewState()
    object Loading : SignInViewState()
    data class SignInInformationInvalid(val result: SignInInformationValidationResult.ValidationFailed) : SignInViewState()
    object SignInFailed : SignInViewState()
    data class SignInSuccess(val instanceInformation: OctoPrintInstanceInformationV2, val warnings: List<SignInUseCase.Warning>) : SignInViewState()
}