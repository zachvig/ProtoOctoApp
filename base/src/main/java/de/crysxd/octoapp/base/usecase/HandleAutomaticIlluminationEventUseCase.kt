package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class HandleAutomaticIlluminationEventUseCase @Inject constructor(
    private val getPowerDevicesUseCase: GetPowerDevicesUseCase,
    private val octoPreferences: OctoPreferences
) : UseCase<HandleAutomaticIlluminationEventUseCase.Event, Boolean>() {

    companion object {
        private var lastJob: Job? = null
    }

    override suspend fun doExecute(param: Event, timber: Timber.Tree): Boolean {
        val autoIds = octoPreferences.automaticLights
        val lights = getPowerDevicesUseCase.execute(GetPowerDevicesUseCase.Params(queryState = false))
            .filter { autoIds.contains(it.first.id) }
            .map { it.first }

        if (lights.isEmpty()) return false

        when (param) {
            Event.WebcamVisible -> timber.i("Turning ${lights.size} on to illuminate webcam")
            Event.WebcamGone -> timber.i("Turning ${lights.size} off")
        }

        // Sync the job to prevent race conditions
        lastJob?.cancel()
        lastJob = GlobalScope.launch {
            lights.forEach {
                when (param) {
                    Event.WebcamVisible -> it.turnOn()
                    Event.WebcamGone -> it.turnOff()
                }
            }
        }

        // Wait for light to be on, but don't wait for it to be off
        if (param is Event.WebcamVisible) {
            lastJob?.join()
        }

        return true
    }

    sealed class Event {
        object WebcamVisible : Event()
        object WebcamGone : Event()
    }
}
