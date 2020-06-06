package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileList
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.printer.*
import retrofit2.Response
import retrofit2.http.*

interface FilesApi {

    @GET("files/{origin}?recursive=true")
    suspend fun getFiles(@Path("origin") origin: FileOrigin): FileList

    @POST("files/{origin}/{path}")
    suspend fun executeFileCommand(@Path("origin") origin: String, @Path("path") path: String, @Body command: Any): Response<Unit>

    class Wrapper(private val wrapped: FilesApi) {

        suspend fun getFiles(origin: FileOrigin): FileList = wrapped.getFiles(origin)

        suspend fun executeFileCommand(file: FileObject.File, command: FileCommand) {
            wrapped.executeFileCommand(file.origin, file.path, command)
        }
    }
}