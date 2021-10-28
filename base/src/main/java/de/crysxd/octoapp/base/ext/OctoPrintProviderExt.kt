package de.crysxd.octoapp.base.ext

import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

suspend fun OctoPrintProvider.awaitFileChange(timeoutMs: Long = 1000) = withTimeoutOrNull(timeoutMs) {
    BaseInjector.get().octoPrintProvider().passiveEventFlow().first {
        it is Event.MessageReceived && it.message is Message.EventMessage.UpdatedFiles
    }
    ""
} != null