package de.crysxd.octoapp.signin.access

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.OctoActivity
import de.crysxd.octoapp.base.data.repository.NotificationIdRepository
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.OpenOctoprintWebUseCase
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import de.crysxd.octoapp.base.utils.PendingIntentCompat
import de.crysxd.octoapp.signin.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber

class RequestAccessViewModel(
    private val requestApiAccessUseCase: RequestApiAccessUseCase,
    private val openOctoprintWebUseCase: OpenOctoprintWebUseCase,
    private val notificationIdRepository: NotificationIdRepository,
) : BaseViewModel() {

    companion object {
        private const val REQUEST_GRACE_PERIOD_MS = 90_000L
    }

    private val notificationManager = BaseInjector.get().context().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val webUrlChannel = MutableStateFlow<HttpUrl?>(null)
    private val mutableUiState = MutableLiveData<UiState>(UiState.PendingApproval)
    private var pollingJob: Job? = null
    private var cancelPollingJob: Job? = null
    val uiState = mutableUiState.asFlow()
        .onStart {
            startPollingJob()
            notificationManager.cancel(notificationIdRepository.requestAccessCompletedNotificationId)
        }.onCompletion {
            scheduleCancelPollingJob()
        }.asLiveData()

    private fun scheduleCancelPollingJob() {
        cancelPollingJob?.cancel()
        cancelPollingJob = viewModelScope.launch(coroutineExceptionHandler) {
            Timber.i("Cancelling polling job in ${REQUEST_GRACE_PERIOD_MS}ms")
            delay(REQUEST_GRACE_PERIOD_MS)
            Timber.i("Cancelling polling job")
            pollingJob?.cancel()
        }
    }

    private fun startPollingJob() {
        // Still active? Nothing to do but cancel the cancellation
        Timber.i("Cancelling polling job cancellation")
        cancelPollingJob?.cancel()
        if (pollingJob?.isActive == true) return

        // We need to collect the flow on the viewModelScope and not as usually done on the viewLifecycle.lifecycleScope because the access request
        // will be cancelled by OctoPrint if there is no request for 5s to check the status. This means if the users leaves the app and the viewLifeCycle.lifecycleScope
        // gets cancelled after 3s, the user only has a total of 8s to accept the request before it's gone. By using the viewModelScope, we keep the requests alive
        Timber.i("Starting new polling job")
        pollingJob = viewModelScope.launch(coroutineExceptionHandler) {
            webUrlChannel.filterNotNull().flatMapLatest {
                requestApiAccessUseCase.execute(RequestApiAccessUseCase.Params(webUrl = it))
            }.map {
                when (it) {
                    is RequestApiAccessUseCase.State.AccessGranted -> UiState.AccessGranted(apiKey = it.apiKey)
                    RequestApiAccessUseCase.State.Failed -> UiState.ManualApiKeyRequired
                    RequestApiAccessUseCase.State.Pending -> UiState.PendingApproval
                }
            }.retry {
                Timber.e(it)
                delay(500)
                true
            }.collect {
                mutableUiState.postValue(it)
                considerShowingNotification(it)
            }
        }
    }

    private fun considerShowingNotification(uiState: UiState) {
        // Attempt to bring activity to front if the user has left the app to grant access
        if (uiState is UiState.AccessGranted && cancelPollingJob?.isActive == true) OctoActivity.instance?.intent?.let { intent ->
            Timber.i("Access granted, but app in background")
            val context = BaseInjector.get().localizedContext()
            val channelId = context.getString(R.string.updates_notification_channel)
            notificationManager.notify(
                notificationIdRepository.requestAccessCompletedNotificationId,
                NotificationCompat.Builder(BaseInjector.get().context(), channelId)
                    .setContentTitle("OctoApp is ready!")
                    .setContentText("Tap here to return")
                    .setAutoCancel(true)
                    .setColorized(true)
                    .setSmallIcon(R.drawable.ic_notification_default)
                    .setContentIntent(PendingIntent.getActivity(BaseInjector.get().context(), 23, intent, PendingIntentCompat.FLAG_UPDATE_CURRENT_IMMUTABLE))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .build()
            )
        }
    }

    fun useWebUrl(webUrl: String) = viewModelScope.launch(coroutineExceptionHandler) {
        if (webUrlChannel.value == null) {
            webUrlChannel.value = webUrl.toHttpUrl()
        }
    }

    fun openInWeb(url: String) = viewModelScope.launch(coroutineExceptionHandler) {
        openOctoprintWebUseCase.execute(OpenOctoprintWebUseCase.Params(octoPrintWebUrl = url.toHttpUrl()))
    }

    sealed class UiState {
        object ManualApiKeyRequired : UiState()
        object PendingApproval : UiState()
        data class AccessGranted(val apiKey: String) : UiState()
    }
}