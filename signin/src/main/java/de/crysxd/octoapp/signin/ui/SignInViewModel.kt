package de.crysxd.octoapp.signin.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult
import de.crysxd.octoapp.signin.models.SignInViewState
import de.crysxd.octoapp.signin.usecases.SignInUseCase
import de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel(
    private val verifyUseCase: VerifySignInInformationUseCase,
    private val signInUseCase: SignInUseCase
) : BaseViewModel() {

    private val mutableViewState = MutableLiveData<SignInViewState>()
    val viewState = Transformations.map(mutableViewState) { it }

    fun startSignIn(info: SignInInformation) =
        viewModelScope.launch(coroutineExceptionHandler) {
            when (val res = verifyUseCase.execute(info)) {
                is SignInInformationValidationResult.ValidationOk -> {
                    mutableViewState.postValue(SignInViewState.Loading)
                    if (!signInUseCase.execute(info)) {
                        mutableViewState.postValue(SignInViewState.SignInFailed)
                    } else {
                        // Sign in success
                    }
                }

                is SignInInformationValidationResult.ValidationFailed -> {
                    mutableViewState.postValue(SignInViewState.SignInInformationInvalid(res))
                }
            }
        }
}