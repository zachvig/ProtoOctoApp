package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.job.Job
import de.crysxd.octoapp.octoprint.models.job.JobCommand
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface JobApi {

    @POST("job")
    suspend fun executeJobCommand(@Body command: Any): Response<Unit>

    @GET("job")
    suspend fun getJob(): Job

    class Wrapper(private val wrapped: JobApi, private val webSocket: EventWebSocket) {

        suspend fun getJob() = wrapped.getJob()

        suspend fun executeJobCommand(command: JobCommand) {
            webSocket.postCurrentMessageInterpolation {
                val flags = it.state?.flags
                when (command) {
                    is JobCommand.CancelJobCommand -> flags?.copy(cancelling = true)
                    is JobCommand.ResumeJobCommand -> flags?.copy(paused = false, pausing = false)
                    is JobCommand.PauseJobCommand -> flags?.copy(pausing = true, paused = false)
                    is JobCommand.TogglePauseCommand -> flags?.copy(pausing = !flags.paused, paused = false)
                    is JobCommand.StartJobCommand -> flags?.copy(printing = true, cancelling = false)
                    else -> flags
                }?.let { updatedFlags ->
                    it.copy(state = it.state?.copy(flags = updatedFlags))
                }
            }

            wrapped.executeJobCommand(command)
        }
    }
}