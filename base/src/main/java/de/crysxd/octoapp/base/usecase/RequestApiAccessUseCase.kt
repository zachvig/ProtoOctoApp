package de.crysxd.octoapp.base.usecase

import android.content.Context
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.octoprint.plugins.applicationkeys.RequestStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject

class RequestApiAccessUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val context: Context
) : UseCase<RequestApiAccessUseCase.Params, Flow<RequestApiAccessUseCase.State>>() {

    companion object {
        private const val POLLING_INTERVAL = 500L
        private const val MAX_FAILURES_BEFORE_RESET = 2
    }

    override suspend fun doExecute(param: Params, timber: Timber.Tree): Flow<State> {
        val octoprint = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV3(id = "adhoc", webUrl = param.webUrl, apiKey = ""))
        val api = octoprint.createApplicationKeysPluginApi()

        return flow {
            emit(State.Pending)

            // Check if supported
            if (!api.probe()) {
                emit(State.Failed)
                return@flow
            }

            while (true) {
                val request = api.request(context.getString(R.string.app_name))

                var consecutiveFailures = 0
                while (true) {
                    // Polling interval
                    delay(POLLING_INTERVAL)

                    try {
                        when (val status = api.checkStatus(request)) {
                            RequestStatus.Pending -> Unit

                            RequestStatus.DeniedOrTimedOut -> {
                                // Wait a polling interval and restart
                                delay(POLLING_INTERVAL)
                                break
                            }

                            is RequestStatus.Granted -> {
                                emit(State.AccessGranted(apiKey = status.apiKey))
                                return@flow
                            }
                        }
                        consecutiveFailures = 0
                    } catch (e: Exception) {
                        consecutiveFailures++
                        if (consecutiveFailures >= MAX_FAILURES_BEFORE_RESET) {
                            delay(POLLING_INTERVAL)
                            break
                        }
                    }
                }
            }
        }.retry(2).catch {
            emit(State.Failed)
        }
    }

    data class Params(
        val webUrl: HttpUrl
    )

    sealed class State {
        object Pending : State()
        object Failed : State()
        data class AccessGranted(val apiKey: String) : State()
    }
}