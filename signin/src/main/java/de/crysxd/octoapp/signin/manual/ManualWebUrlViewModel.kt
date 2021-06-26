package de.crysxd.octoapp.signin.manual

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.ui.base.BaseViewModel

class ManualWebUrlViewModel(
    private val sensitiveDataMask: SensitiveDataMask,
) : BaseViewModel() {

    private val mutbaleViewState = MutableLiveData<ViewState>()
    val viewState = mutbaleViewState.map { it }

    fun testWebUrl(webUrl: String) {
        val upgradedWebUrl = if (!webUrl.startsWith("http://") && !webUrl.startsWith("https://")) {
            "http://${webUrl}"
        } else {
            webUrl
        }
        sensitiveDataMask.registerWebUrl(upgradedWebUrl, "manually_entered_web_url")

        try {
            if (webUrl.isBlank()) {
                throw IllegalArgumentException("URL is empty")
            }
            Uri.parse(upgradedWebUrl)
            mutbaleViewState.postValue(ViewState.Success(webUrl = webUrl))
        } catch (e: Exception) {
            mutbaleViewState.postValue(ViewState.Error(message = "Please provide a valid URL H", exception = e))
        }
    }

    sealed class ViewState {
        data class Success(val webUrl: String) : ViewState()
        data class Error(var handled: Boolean = false, val message: String? = null, val exception: Exception) : ViewState()
    }
}