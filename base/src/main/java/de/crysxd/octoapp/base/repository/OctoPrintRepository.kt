package de.crysxd.octoapp.base.repository

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
class OctoPrintRepository(
    private val dataSource: DataSource<OctoPrintInstanceInformationV2>,
    private val checkOctoPrintInstanceInformationUseCase: CheckOctoPrintInstanceInformationUseCase
) {

    val instanceInformationChannel = ConflatedBroadcastChannel<OctoPrintInstanceInformationV2?>(null)

    @Deprecated("Use instanceInformationFlow()")
    val instanceInformation = instanceInformationChannel.asFlow().asLiveData()

    init {
        storeOctoprintInstanceInformation(getRawOctoPrintInstanceInformation())
    }

    fun instanceInformationFlow() = instanceInformationChannel.asFlow()

    fun clearOctoprintInstanceInformation(apiKeyWasInvalid: Boolean = false) {
        val currentValue = getRawOctoPrintInstanceInformation()
        storeOctoprintInstanceInformation(
            if (apiKeyWasInvalid) {
                currentValue?.copy(apiKey = "", apiKeyWasInvalid = true)
            } else {
                null
            }
        )
    }

    fun storeOctoprintInstanceInformation(instance: OctoPrintInstanceInformationV2?) = GlobalScope.launch {
        if (instance == null || instance != instanceInformationChannel.valueOrNull) {
            dataSource.store(instance)
            Timber.i("Posting $instance")
            instanceInformationChannel.offer(
                if (instance != null && checkOctoPrintInstanceInformationUseCase.execute(instance)) {
                    instance
                } else {
                    null
                }
            )
        }
    }

    fun getRawOctoPrintInstanceInformation() = dataSource.get()
}