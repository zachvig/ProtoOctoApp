package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.models.files.FileList
import retrofit2.http.GET

interface FilesApi {

    @GET("files")
    suspend fun getFiles(): FileList

}