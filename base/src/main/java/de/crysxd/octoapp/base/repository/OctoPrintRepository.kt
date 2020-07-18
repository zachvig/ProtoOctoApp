package de.crysxd.octoapp.base.repository

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class OctoPrintRepository(
    private val dataSource: DataSource<OctoPrintInstanceInformation>,
    private val checkOctoPrintInstanceInformationUseCase: CheckOctoPrintInstanceInformationUseCase
) {

    private val mutableInstanceInformation = MutableLiveData<OctoPrintInstanceInformation?>()
    val instanceInformation = Transformations.map(mutableInstanceInformation) { it }

    init {
        storeOctoprintInstanceInformation(getOctoprintInstanceInformation())
    }

    fun clearOctoprintInstanceInformation() {
        storeOctoprintInstanceInformation(OctoPrintInstanceInformation("", -1, ""))
    }

    fun storeOctoprintInstanceInformation(instance: OctoPrintInstanceInformation?) = GlobalScope.launch {
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

    private fun getOctoprintInstanceInformation() = dataSource.get()
}