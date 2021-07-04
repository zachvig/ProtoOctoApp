package de.crysxd.octoapp.signin.access

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import de.crysxd.octoapp.signin.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class RequestAccessViewModel(
    private val requestApiAccessUseCase: RequestApiAccessUseCase,
    context: Context
) : BaseViewModel() {

    companion object {
        private const val REQUEST_GRACE_PERIOD_MS = 900_000L
        private const val NOTIFICATION_ID = 3432
    }

    private val appContext = context.applicationContext
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val webUrlChannel = ConflatedBroadcastChannel<String>()
    private val mutableUiState = MutableLiveData<UiState>(UiState.PendingApproval)
    private var pollingJob: Job? = null
    private var cancelPollingJob: Job? = null
    val uiState = mutableUiState.asFlow()
        .onStart {
            startPollingJob()
            notificationManager.cancel(NOTIFICATION_ID)
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
            webUrlChannel.asFlow().flatMapLatest {
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

                // Attempt to bring activity to front if the user has left the app to grant access
                if (it is UiState.AccessGranted && cancelPollingJob?.isActive == true) OctoActivity.instance?.intent?.let { intent ->
                    Timber.i("Access granted, but app in background")
                    val channelId = "alerts"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        notificationManager.createNotificationChannel(NotificationChannel(channelId, "Alerts", NotificationManager.IMPORTANCE_MAX))
                    }
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        NotificationCompat.Builder(appContext, channelId)
                            .setContentTitle("OctoApp is ready!")
                            .setContentText("Tap here to return")
                            .setAutoCancel(true)
                            .setColorized(true)
                            .setSmallIcon(R.drawable.ic_notification_default)
                            .setContentIntent(PendingIntent.getActivity(appContext, 23, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .build()
                    )
                }
            }
        }
    }

    fun useWebUrl(webUrl: String) = if (webUrlChannel.valueOrNull == null) {
        webUrlChannel.offer(webUrl)
    } else {
        false
    }

    sealed class UiState {
        object ManualApiKeyRequired : UiState()
        object PendingApproval : UiState()
        data class AccessGranted(val apiKey: String) : UiState()
    }
}