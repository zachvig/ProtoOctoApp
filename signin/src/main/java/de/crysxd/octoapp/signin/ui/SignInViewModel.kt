package de.crysxd.octoapp.signin.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult
import de.crysxd.octoapp.signin.usecases.SignInUseCase
import de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInViewModel(
    private val verifyUseCase: VerifySignInInformationUseCase,
    private val signInUseCase: SignInUseCase
) : BaseViewModel() {

    private val mutableVerificationStatus = MutableLiveData<SignInInformationValidationResult>()
    val verificationStatus = Transformations.map(mutableVerificationStatus) { it }


    fun startSignIn(info: SignInInformation) =
        viewModelScope.launch(coroutineExceptionHandler) {
            val res = verifyUseCase.execute(info)
            mutableVerificationStatus.postValue(res)

            if (res == SignInInformationValidationResult.ValidationOk) {
                signInUseCase.execute(info)
            }
        }
}