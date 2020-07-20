package de.crysxd.octoapp.base.feedback

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.OpenEmailClientForFeedbackUseCase
import kotlinx.coroutines.launch

class SendFeedbackViewModel(
    private val sendFeedbackUseCase: OpenEmailClientForFeedbackUseCase,
    private val cacheTree: TimberCacheTree
) : BaseViewModel() {

    var screenshot: Bitmap? = null

    fun sendFeedback(context: Context, sendLogs: Boolean, sendScreenshot: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        val logs = if (sendLogs) {
            cacheTree.logs
        } else {
            null
        }
        val screenshot = if (sendScreenshot) {
            screenshot
        } else {
            null
        }

        sendFeedbackUseCase.execute(OpenEmailClientForFeedbackUseCase.Params(context, logs, screenshot))
    }
}