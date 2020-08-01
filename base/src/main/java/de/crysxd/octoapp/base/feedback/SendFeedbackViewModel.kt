package de.crysxd.octoapp.base.feedback

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.OpenEmailClientForFeedbackUseCase
import kotlinx.coroutines.launch

class SendFeedbackViewModel(
    private val sendFeedbackUseCase: OpenEmailClientForFeedbackUseCase
) : BaseViewModel() {

    var screenshot: Bitmap? = null

    fun sendFeedback(
        context: Context,
        sendPhoneInfo: Boolean,
        sendOctoPrintInfo: Boolean,
        sendLogs: Boolean,
        sendScreenshot: Boolean
    ) = viewModelScope.launch(coroutineExceptionHandler) {

        val screenshot = if (sendScreenshot) {
            screenshot
        } else {
            null
        }

        sendFeedbackUseCase.execute(
            OpenEmailClientForFeedbackUseCase.Params(
                context = context,
                sendPhoneInfo = sendPhoneInfo,
                sendOctoPrintInfo = sendOctoPrintInfo,
                sendLogs = sendLogs,
                screenshot = screenshot
            )
        )
    }
}