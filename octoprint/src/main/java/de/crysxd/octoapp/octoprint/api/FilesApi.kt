package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileList
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FilesApi {

    @GET("files/{origin}?recursive=true")
    suspend fun getAllFiles(@Path("origin") origin: FileOrigin): FileList

    @GET("files/{origin}")
    suspend fun getRootFolder(@Path("origin") origin: FileOrigin): FileList

    @GET("files/{origin}/{path}")
    suspend fun getSubFolder(@Path("origin") origin: FileOrigin, @Path("path") path: String): FileObject.Folder

    @POST("files/{origin}/{path}")
    suspend fun executeFileCommand(@Path("origin") origin: String, @Path("path") path: String, @Body command: Any): Response<Unit>

    class Wrapper(private val wrapped: FilesApi) {

        suspend fun getAllFiles(origin: FileOrigin): FileList = wrapped.getAllFiles(origin)

        suspend fun getFiles(origin: FileOrigin, folder: FileObject.Folder?): FileList = if (folder != null) {
            FileList(files = wrapped.getSubFolder(origin, folder.path).children ?: emptyList())
        } else {
            wrapped.getRootFolder(origin)
        }

        suspend fun executeFileCommand(file: FileObject.File, command: FileCommand) {
            wrapped.executeFileCommand(file.origin, file.path, command)
        }
    }
}