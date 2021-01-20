package de.crysxd.octoapp.signin.ui

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.logging.SensitiveDataMask
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
    private val signInUseCase: SignInUseCase,
    private val sensitiveDataMask: SensitiveDataMask
) : BaseViewModel() {

    var failedSignInCounter = 0
    var invalidApiKeyInfoWasShown: Boolean = false
    private val mutableViewState = MutableLiveData<SignInViewState>()
    val viewState = Transformations.map(mutableViewState) { it }

    fun startSignIn(info: SignInInformation) =
        viewModelScope.launch(coroutineExceptionHandler) {
            // Register sensitive data to exclude it from logs
            sensitiveDataMask.registerApiKey(info.apiKey)
            sensitiveDataMask.registerWebUrl(info.webUrl)

            val upgradedInfo = info.copy(
                webUrl = addHttpIfNotPresent(info.webUrl)
            )

            try {
                when (val res = verifyUseCase.execute(upgradedInfo)) {
                    is SignInInformationValidationResult.ValidationOk -> {
                        mutableViewState.postValue(SignInViewState.Loading)
                        mutableViewState.postValue(
                            when (val result = signInUseCase.execute(upgradedInfo)) {
                                is SignInUseCase.Result.Success -> {
                                    failedSignInCounter = 0
                                    SignInViewState.SignInSuccess(result.octoPrintInstanceInformation, result.warnings)
                                }
                                is SignInUseCase.Result.Failure -> SignInViewState.SignInFailed(
                                    result.exception,
                                    ++failedSignInCounter,
                                    Uri.parse(upgradedInfo.webUrl),
                                    upgradedInfo.apiKey
                                )
                            }
                        )
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

    private fun addHttpIfNotPresent(webUrl: String) = if (!webUrl.startsWith("http://") && !webUrl.startsWith("https://")) {
        "http://$webUrl"
    } else {
        webUrl
    }

    fun completeSignIn(instanceInformation: OctoPrintInstanceInformationV2) {
        // Save instance information, MainActivity will navigate away
        OctoAnalytics.logEvent(OctoAnalytics.Event.Login)
        octoPrintRepository.storeOctoprintInstanceInformation(instanceInformation)
    }

    fun getPreFillInfo() = octoPrintRepository.getRawOctoPrintInstanceInformation() ?: OctoPrintInstanceInformationV2(
        webUrl = "",
        apiKey = ""
    )
}