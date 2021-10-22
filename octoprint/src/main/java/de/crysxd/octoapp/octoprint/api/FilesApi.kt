package de.crysxd.octoapp.octoprint.api

import de.crysxd.octoapp.octoprint.ProgressRequestBody.Companion.asProgressRequestBody
import de.crysxd.octoapp.octoprint.models.files.FileCommand
import de.crysxd.octoapp.octoprint.models.files.FileList
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.resolvePath
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface FilesApi {

    @GET("files/{origin}?recursive=true")
    suspend fun getAllFiles(@Path("origin") origin: FileOrigin): FileList

    @GET("files/{origin}")
    suspend fun getRootFolder(@Path("origin") origin: FileOrigin): FileList

    @GET("files/{origin}/{path}")
    suspend fun getSubFolder(@Path("origin") origin: FileOrigin, @Path("path") path: String): FileObject.Folder

    @POST("files/{origin}/{path}")
    suspend fun executeFileCommand(@Path("origin") origin: FileOrigin, @Path("path") path: String, @Body command: Any): Response<Unit>

    @DELETE("files/{origin}/{path}")
    suspend fun deleteFile(@Path("origin") origin: FileOrigin, @Path("path") path: String): Response<Unit>

    @POST("files/{origin}")
    suspend fun uploadObject(@Path("origin") origin: FileOrigin, @Body body: MultipartBody): Response<Unit>

    class Wrapper(
        private val webUrl: HttpUrl,
        private val okHttpClient: OkHttpClient,
        private val wrapped: FilesApi
    ) {
        fun downloadFile(file: FileObject.File): InputStream? {
            val request = Request.Builder()
                .get()
                .url(webUrl.resolvePath("downloads/files/${file.origin}/${file.path}"))
                .build()

            // Use a extended read timeout. This is a fix for OctoEverywhere where it can take a minute before
            // any data is received
            return okHttpClient.newBuilder()
                .readTimeout(5, TimeUnit.MINUTES)
                .callTimeout(10, TimeUnit.MINUTES)
                .build()
                .newCall(request)
                .execute().body?.byteStream()
        }

        suspend fun getAllFiles(origin: FileOrigin): FileList = wrapped.getAllFiles(origin)

        suspend fun deleteFile(file: FileObject) {
            wrapped.deleteFile(origin = file.origin, path = file.path)
        }

        suspend fun createFolder(origin: FileOrigin, parent: FileObject.Folder?, name: String) {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(name = "foldername", value = name)
                .addFormDataPart(name = "path", value = parent?.path ?: "/")
                .build()

            wrapped.uploadObject(origin, body)
        }

        suspend fun uploadFile(origin: FileOrigin, source: File, name: String, parent: FileObject.Folder?, progressUpdate: (Float) -> Unit) {
            val fileBody = source.asRequestBody("application/octet-stream".toMediaType()).asProgressRequestBody(progressUpdate)
            val filePart = MultipartBody.Part.createFormData(name = "file", filename = name, body = fileBody)
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(filePart)
                .addFormDataPart(name = "path", value = parent?.path ?: "/")
                .build()

            wrapped.uploadObject(origin, body)
        }

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