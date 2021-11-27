package de.crysxd.baseui.common.configureremote

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.isSpaghettiDetectiveUrl
import de.crysxd.octoapp.octoprint.plugins.thespaghettidetective.SpaghettiDetectiveDataUsage
import kotlinx.coroutines.launch
import timber.log.Timber

class ConfigureRemoteAccessSpaghettiDetectiveViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
) : ViewModel() {

    private val mutableDataUsage = MutableLiveData<DataUsageWrapper>(DataUsageWrapper.Loading)
    val dataUsage = mutableDataUsage.map { it }

    fun fetchDataUsage() = viewModelScope.launch {
        try {
            mutableDataUsage.postValue(DataUsageWrapper.Loading)
            val octoPrint = octoPrintRepository.getActiveInstanceSnapshot() ?: throw IllegalStateException("No active instance")
            val spaghettiUrl = octoPrint.alternativeWebUrl
            require(spaghettiUrl != null && spaghettiUrl.isSpaghettiDetectiveUrl()) { "Alternative url is not Spaghetti Detective " }
            val d = octoPrintProvider.createAdHocOctoPrint(octoPrint.copy(webUrl = spaghettiUrl, alternativeWebUrl = null)).createSpaghettiDetectiveApi().getDataUsage()
            mutableDataUsage.postValue(DataUsageWrapper.Data(d))
        } catch (e: Exception) {
            Timber.e(e)
            mutableDataUsage.postValue(DataUsageWrapper.Failed)
        }
    }

    sealed class DataUsageWrapper {
        object Loading : DataUsageWrapper()
        object Failed : DataUsageWrapper()
        data class Data(val dataUsage: SpaghettiDetectiveDataUsage) : DataUsageWrapper()
    }
}