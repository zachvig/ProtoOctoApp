package de.crysxd.baseui.common.controlcenter

import android.graphics.Bitmap
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.databinding.ControlCenterFragmentBinding
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ext.filterEventsForMessageType
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.GetWebcamSnapshotUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
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
    private val getWebcamSnapshotUseCase: GetWebcamSnapshotUseCase,
) : BaseViewModel() {

    var viewPool: ControlCenterFragmentBinding? = null
    var viewPoolDark: Boolean? = null
    private var loadCurrentMessagesJob: Job? = null
    private var loadSnapshotsJob: Job? = null
    private val currentMessageFlow = MutableStateFlow<Map<String, Message.CurrentMessage?>>(emptyMap())
    private val snapshotsFlow = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
    val viewState = octoPrintRepository.instanceInformationFlow().map { active ->
        InstanceList(
            activeId = active?.id,
            instances = octoPrintRepository.getAll().map { Instance(info = it) }
        )
    }.combine(snapshotsFlow) { data, snapshots ->
        data.copy(instances = data.instances.map { i -> i.copy(snapshot = snapshots[i.info.id]) })
    }.combine(currentMessageFlow) { data, messages ->
        data.copy(instances = data.instances.map { i -> i.copy(lastMessage = messages[i.info.id]) })
    }.onStart {
        loadCurrentMessagesJob?.cancel()
        loadCurrentMessagesJob = loadCurrentMessages()
        loadSnapshotsJob?.cancel()
        loadSnapshotsJob = loadWebcamSnapshots()
        delay(300)
    }.onCompletion {
        loadSnapshotsJob?.cancel()
        loadCurrentMessagesJob?.cancel()
    }.rateLimit(500).asLiveData()


    private fun getCurrentMessages(instanceId: String) = octoPrintProvider.eventFlow(
        tag = "control-center",
        instanceId = instanceId,
        config = EventFlowConfiguration(throttle = 5)
    ).filterEventsForMessageType<Message.CurrentMessage>().let {
        flow {
            emit(instanceId to null)
            emitAll(it.map { i -> instanceId to i })
        }
    }

    private fun getSnapshot(instanceId: String) = flow {
        emit(instanceId to null)
        val info = octoPrintRepository.get(instanceId)
        val bitmap = getWebcamSnapshotUseCase.execute(
            GetWebcamSnapshotUseCase.Params(
                instanceInfo = info,
                maxSizePx = 300,
                cornerRadiusPx = 0f,
                illuminateIfPossible = true,
                sampleRateMs = 1_000
            )
        ).first()
        emit(instanceId to bitmap)
    }.catch {
        Timber.w("Unable to load snapshot for $instanceId: ${it::class.simpleName}: ${it.message}")
    }

    private fun loadWebcamSnapshots(): Job {
        val flows = octoPrintRepository.getAll().map { instance -> getSnapshot(instance.id) }
        val combined = combine(flows) { it.toMap() }
        return viewModelScope.launch {
            combined.onEach {
                snapshotsFlow.emit(it)
            }.onStart {
                Timber.i("Starting to load snapshots")
            }.onCompletion {
                Timber.i("Stopping to load snapshots")
            }.catch {
                Timber.e(it)
            }.collect()
        }
    }

    private fun loadCurrentMessages(): Job {
        val flows = octoPrintRepository.getAll().map { instance -> getCurrentMessages(instance.id) }
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
        val lastMessage: Message.CurrentMessage? = null,
        val snapshot: Bitmap? = null,
    )
}