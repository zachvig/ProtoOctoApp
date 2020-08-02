package de.crysxd.octoapp.base.repository

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class OctoPrintRepository(
    private val dataSource: DataSource<OctoPrintInstanceInformationV2>,
    private val checkOctoPrintInstanceInformationUseCase: CheckOctoPrintInstanceInformationUseCase
) {

    private val mutableInstanceInformation = MutableLiveData<OctoPrintInstanceInformationV2?>()
    val instanceInformation = Transformations.map(mutableInstanceInformation) { it }

    init {
        storeOctoprintInstanceInformation(getRawOctoPrintInstanceInformation())
    }

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
        if (instance == null || instance != instanceInformation.value) {
            dataSource.store(instance)
            mutableInstanceInformation.postValue(
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