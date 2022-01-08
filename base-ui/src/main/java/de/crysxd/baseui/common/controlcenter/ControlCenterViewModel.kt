package de.crysxd.baseui.common.controlcenter

import androidx.lifecycle.asLiveData
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class ControlCenterViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    octoPrintProvider: OctoPrintProvider,
) : BaseViewModel() {

    val viewState = octoPrintRepository.instanceInformationFlow().flatMapLatest { _ ->
        val flows = octoPrintRepository.getAll().map { instance ->
            flow {
                emit(null)
                val cmFlow = octoPrintProvider.eventFlow(tag = "control-center", instanceId = instance.id, config = EventFlowConfiguration(throttle = 5))
                    .mapNotNull { (it as? Event.MessageReceived)?.message as? Message.CurrentMessage }
                emitAll(cmFlow)
            }.map {
                Instance(info = instance, lastMessage = it)
            }
        }

        val instanceFlows = combine(flows) { it.toList() }.rateLimit(300)
        instanceFlows.combine(octoPrintRepository.instanceInformationFlow().map { it?.id }.distinctUntilChanged()) { instances, activeId ->
            InstanceList(
                instances = instances,
                activeId = activeId,
            )
        }
    }.asLiveData()

    fun active(instance: Instance) {
        octoPrintRepository.setActive(instance.info)
    }

    data class InstanceList(
        val instances: List<Instance>,
        val activeId: String?,
    )

    data class Instance(
        val info: OctoPrintInstanceInformationV3,
        val lastMessage: Message.CurrentMessage?,
    )
}