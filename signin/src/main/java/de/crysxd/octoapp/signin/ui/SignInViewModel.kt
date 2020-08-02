package de.crysxd.octoapp.signin.ui

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.signin.models.SignInInformation
import de.crysxd.octoapp.signin.models.SignInInformationValidationResult
import de.crysxd.octoapp.signin.models.SignInViewState
import de.crysxd.octoapp.signin.usecases.SignInUseCase
import de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase
import kotlinx.coroutines.launch

class SignInViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val verifyUseCase: VerifySignInInformationUseCase,
    private val signInUseCase: SignInUseCase
) : BaseViewModel() {

    var invalidApiKeyInfoWasShown: Boolean = false
    private val mutableViewState = MutableLiveData<SignInViewState>()
    val viewState = Transformations.map(mutableViewState) { it }

    fun startSignIn(info: SignInInformation) =
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                when (val res = verifyUseCase.execute(info)) {
                    is SignInInformationValidationResult.ValidationOk -> {
                        mutableViewState.postValue(SignInViewState.Loading)
                        val result = signInUseCase.execute(info)
                        if (result is SignInUseCase.Result.Success) {
                            mutableViewState.postValue(SignInViewState.SignInSuccess(result.octoPrintInstanceInformation, result.warnings))
                        } else {
                            mutableViewState.postValue(SignInViewState.SignInFailed)
                        }
                    }

                    is SignInInformationValidationResult.ValidationFailed -> {
                        mutableViewState.postValue(SignInViewState.SignInInformationInvalid(res))
                    }
                }
            } catch (e: Throwable) {
                mutableViewState.postValue(SignInViewState.Idle)
                throw e
            }
        }

    fun completeSignIn(instanceInformation: OctoPrintInstanceInformationV2) {
        // Save instance information, MainActivity will navigate away
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle.EMPTY)
        octoPrintRepository.storeOctoprintInstanceInformation(instanceInformation)
    }

    fun getPreFillInfo() = octoPrintRepository.getRawOctoPrintInstanceInformation() ?: OctoPrintInstanceInformationV2(
        webUrl = "",
        apiKey = ""
    )
}