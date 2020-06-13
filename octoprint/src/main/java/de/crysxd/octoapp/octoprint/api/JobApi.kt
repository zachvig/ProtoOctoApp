package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.job.JobCommand
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface JobApi {

    @POST("job")
    suspend fun executeJobCommand(@Body command: Any): Response<Unit>

    class Wrapper(private val wrapped: JobApi) {

        suspend fun executeJobCommand(command: JobCommand) {
            wrapped.executeJobCommand(command)
        }
    }
}