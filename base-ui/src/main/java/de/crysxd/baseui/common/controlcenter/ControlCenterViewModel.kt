package de.crysxd.baseui.common.controlcenter

import androidx.lifecycle.asLiveData
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import kotlinx.coroutines.flow.map

class ControlCenterViewModel(
    octoPrintRepository: OctoPrintRepository,
) : BaseViewModel() {

    val viewState = octoPrintRepository.instanceInformationFlow().map { _ ->
        octoPrintRepository.getAll().map { it to null }
    }.asLiveData()
}