package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.files.FileList
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import retrofit2.http.GET
import retrofit2.http.Path

interface FilesApi {

    @GET("files/{origin}?recursive=true")
    suspend fun getFiles(@Path("origin") origin: FileOrigin): FileList

}