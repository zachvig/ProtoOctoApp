package de.crysxd.octoapp.base.feedback

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.usecase.OpenEmailClientForFeedbackUseCase
import kotlinx.coroutines.launch

class SendFeedbackViewModel(
    private val sendFeedbackUseCase: OpenEmailClientForFeedbackUseCase
) : BaseViewModel() {

    val viewState = MutableLiveData<ViewState>(ViewState.Idle)

    fun sendFeedback(
        context: Context,
        message: String,
        sendPhoneInfo: Boolean,
        sendOctoPrintInfo: Boolean,
        sendLogs: Boolean,
    ) = viewModelScope.launch(coroutineExceptionHandler) {
        viewState.postValue(ViewState.Loading)
        sendFeedbackUseCase.execute(
            OpenEmailClientForFeedbackUseCase.Params(
                message = message,
                context = context,
                sendPhoneInfo = sendPhoneInfo,
                sendOctoPrintInfo = sendOctoPrintInfo,
                sendLogs = sendLogs,
                screenshot = null
            )
        )
    }.invokeOnCompletion {
        viewState.postValue(ViewState.Done)
    }

    sealed class ViewState {
        object Idle : ViewState()
        object Loading : ViewState()
        object Done : ViewState()
    }
}