package de.crysxd.octoapp.ui.signin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.models.SignInInformation
import de.crysxd.models.SignInInformationValidationResult
import de.crysxd.octoapp.ui.BaseViewModel
import de.crysxd.usecases.signin.SignInUseCase
import de.crysxd.usecases.signin.VerifySignInInformationUseCase
import kotlinx.coroutines.launch

class SignInViewModel(
    private val verifyUseCase: VerifySignInInformationUseCase,
    private val signInUseCase: SignInUseCase
) : BaseViewModel() {

    private val mutableVerificationStatus = MutableLiveData<SignInInformationValidationResult>()
    val verificationStatus = Transformations.map(mutableVerificationStatus) { it }


    fun startSignIn(info: SignInInformation) = viewModelScope.launch(coroutineExceptionHandler) {
        val res = verifyUseCase.execute(info)
        mutableVerificationStatus.postValue(res)

        if (res == SignInInformationValidationResult.ValidationOk) {
            signInUseCase.execute(info)
        }
    }
}