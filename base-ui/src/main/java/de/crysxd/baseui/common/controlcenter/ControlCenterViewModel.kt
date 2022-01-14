package de.crysxd.baseui.common.controlcenter

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.databinding.ControlCenterFragmentBinding
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ext.filterEventsForMessageType
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber

class ControlCenterViewModel(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider,
) : BaseViewModel() {

    var viewPool: ControlCenterFragmentBinding? = null
    private var loadCurrentMessagesJob: Job? = null
    private val currentMessageFlow = MutableStateFlow<Map<String, Message.CurrentMessage?>>(emptyMap())
    val viewState = octoPrintRepository.instanceInformationFlow().map { active ->
        octoPrintRepository.getAll() to active?.id
    }.combine(currentMessageFlow) { instances, messages ->
        InstanceList(
            activeId = instances.second,
            instances = instances.first.map {
                Instance(
                    info = it,
                    lastMessage = messages[it.id]
                )
            }
        )
    }.onStart {
        loadCurrentMessagesJob?.cancel()
        loadCurrentMessagesJob = loadCurrentMessages()
    }.onCompletion {
        loadCurrentMessagesJob?.cancel()
    }.asLiveData()


    private fun getFlow(instanceId: String) = octoPrintProvider.eventFlow(
        tag = "control-center",
        instanceId = instanceId,
        config = EventFlowConfiguration(throttle = 5)
    ).filterEventsForMessageType<Message.CurrentMessage>().let {
        flow {
            emit(instanceId to null)
            emitAll(it.map { i -> instanceId to i })
        }
    }

    private fun loadCurrentMessages(): Job {
        val flows = octoPrintRepository.getAll().map { instance -> getFlow(instance.id) }
        val combined = combine(flows) { it.toMap() }

        // We load the current messages on the viewModelScope to keep the flows active while switching the instance
        // This is important to prevent the websocket from reconnecting because we are not observing events briefly
        return viewModelScope.launch {
            combined.onEach {
                currentMessageFlow.emit(it)
            }.onStart {
                Timber.i("Starting to read current messages")
            }.onCompletion {
                Timber.i("Stopping to read current messages")
            }.retry {
                delay(1000)
                Timber.e(it)
                true
            }.collect()
        }
    }

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