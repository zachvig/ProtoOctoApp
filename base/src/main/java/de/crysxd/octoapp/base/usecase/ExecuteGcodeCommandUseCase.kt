package de.crysxd.octoapp.base.usecase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

class ExecuteGcodeCommandUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val gcodeHistoryRepository: GcodeHistoryRepository,
    private val serialCommunicationLogsRepository: SerialCommunicationLogsRepository
) : UseCase<ExecuteGcodeCommandUseCase.Param, List<ExecuteGcodeCommandUseCase.Response>>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) = if (param.recordResponse) {
        when (param.command) {
            is GcodeCommand.Single -> listOf(param.command)
            is GcodeCommand.Batch -> param.command.commands.map { GcodeCommand.Single(it) }
        }.map {
            executeAndRecordResponse(it, param.fromUser, timber)
        }
    } else {
        execute(param.command, param.fromUser, timber)
        when (param.command) {
            is GcodeCommand.Single -> listOf(Response.DroppedResponse)
            is GcodeCommand.Batch -> param.command.commands.map { Response.DroppedResponse }
        }
    }

    private suspend fun executeAndRecordResponse(command: GcodeCommand.Single, fromUser: Boolean, timber: Timber.Tree) = withContext(Dispatchers.Default) {
        val readJob = async {
            var sendLineFound = false
            val sendLinePattern = Pattern.compile("^Send:.*%%COMMAND%%.*".replace("%%COMMAND%%", command.command))
            val responseEndLinePattern = Pattern.compile(Firebase.remoteConfig.getString("gcode_response_end_line_pattern"))
            val spamMessagePattern = Pattern.compile(Firebase.remoteConfig.getString("gcode_response_spam_pattern"))

            // Collect all lines from when we see `Send: $command` until we see `Recv: ok`
            val list = serialCommunicationLogsRepository
                .flow(false)
                .map { it.content }
                .filter {
                    // Filter undesired "Spam"
                    !spamMessagePattern.matcher(it).matches()
                }
                .filter {
                    // Skip until send line was found
                    if (!sendLineFound) {
                        sendLineFound = sendLinePattern.matcher(it).matches()
                        if (sendLineFound) {
                            timber.v("Send line was found: $it")
                        }
                        sendLineFound
                    } else {
                        true
                    }
                }
                .takeWhile {
                    !responseEndLinePattern.matcher(it).matches()
                }
                .toList()

            timber.v("Recorded response for ${command.command}: $list")
            Response.RecordedResponse(
                sendLine = list.first(),
                responseLines = list.subList(1, list.size)
            )
        }

        // Execute command
        execute(command, fromUser, timber)

        // Wait until response is read
        return@withContext readJob.await()
    }

    private suspend fun execute(command: GcodeCommand, fromUser: Boolean, timber: Timber.Tree) {
        when (command) {
            is GcodeCommand.Single -> logExecuted(command.command, fromUser)
            is GcodeCommand.Batch -> command.commands.forEach { logExecuted(it, fromUser) }
        }

        timber.v("Executing $command")
        octoPrintProvider.octoPrint().createPrinterApi().executeGcodeCommand(command)
    }

    private fun logExecuted(command: String, fromUser: Boolean) {
        serialCommunicationLogsRepository.addInternalLog(
            log = "[OctoApp] Send: $command",
            fromUser = fromUser
        )

        if (fromUser) {
            Firebase.analytics.logEvent("gcode_send") {
                param("command", command)
            }
            gcodeHistoryRepository.recordGcodeSend(command)
        }
    }

    sealed class Response {
        data class RecordedResponse(
            val sendLine: String,
            val responseLines: List<String>
        ) : Response()

        object DroppedResponse : Response()
    }

    data class Param(
        val command: GcodeCommand,
        val fromUser: Boolean,
        val recordResponse: Boolean = false
    )
}