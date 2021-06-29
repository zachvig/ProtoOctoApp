package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import javax.inject.Inject

class HandleAutomaticLightEventUseCase @Inject constructor(
    private val getPowerDevicesUseCase: GetPowerDevicesUseCase,
    private val octoPreferences: OctoPreferences
) : UseCase<HandleAutomaticLightEventUseCase.Event, Boolean>() {

    companion object {
        private var lastJob: Job? = null
        private var keepOnCounter = 0
        private val lock = Semaphore(1)
    }

    override suspend fun doExecute(param: Event, timber: Timber.Tree): Boolean = lock.withPermit {
        try {
            if (!BillingManager.isFeatureEnabled(BillingManager.FEATURE_AUTOMATIC_LIGHTS)) {
                timber.i("Automatic lights disabled, skipping any action")
                return false
            }

            val autoIds = octoPreferences.automaticLights
            val lights = getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(queryState = false))
                .filter { autoIds.contains(it.first.id) }
                .map { it.first }

            if (lights.isEmpty()) {
                timber.i("No automatic lights set up, skipping any action")
                return false
            }

            // Determine new state
            val prevCounter = keepOnCounter
            when (param) {
                is Event.WebcamVisible -> keepOnCounter++
                is Event.WebcamGone -> keepOnCounter--
            }.coerceAtLeast(0)

            val newState = when {
                prevCounter == 0 && keepOnCounter > 0 -> true
                prevCounter > 0 && keepOnCounter == 0 -> false
                else -> {
                    timber.d("No action taken, still $keepOnCounter references")
                    null
                }
            }

            // Push new state
            if (newState != null) {
                lastJob?.cancelAndJoin()
                lastJob = GlobalScope.launch {
                    try {
                        if (param.delayAction) {
                            delay(5000)
                        }

                        if (lastJob?.isCancelled == true) {
                            timber.i("Action cancelled: $newState")
                            return@launch
                        }

                        lights.forEach {
                            when (newState) {
                                true -> {
                                    timber.i("☀️ Turning ${lights.size} lights on to illuminate webcam")
                                    it.turnOn()
                                }
                                false -> {
                                    timber.i("🌙 Turning ${lights.size} lights off")
                                    it.turnOff()
                                }
                                null -> Unit
                            }
                        }
                    } catch (e: Exception) {
                        timber.e(e)
                    }
                }
            }

            // Wait for light to be on, but don't wait for it to be off
            if (param is Event.WebcamVisible) {
                lastJob?.join()
            }

            true
        } catch (e: Exception) {
            timber.e(e)
            false
        }
    }

    sealed class Event {
        abstract val source: String
        abstract val delayAction: Boolean

        data class WebcamVisible(override val source: String, override val delayAction: Boolean = false) : Event()
        data class WebcamGone(override val source: String, override val delayAction: Boolean = false) : Event()
    }
}
