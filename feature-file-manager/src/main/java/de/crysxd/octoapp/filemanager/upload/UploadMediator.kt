package de.crysxd.octoapp.filemanager.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.filemanager.di.FileManagerScope
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import javax.inject.Inject


@FileManagerScope
class UploadMediator @Inject constructor(
    private val context: Context,
    private val octoPrintProvider: OctoPrintProvider
) {

    private val contentResolver = context.contentResolver
    private val activeUploads = mutableListOf<Upload>()

    fun getActiveUploads() = activeUploads.toList()

    fun getActiveUploads(origin: FileOrigin, parent: FileObject.Folder?) = activeUploads.filter {
        it.parent?.path == parent?.path && origin == it.origin
    }

    suspend fun startUpload(contentResolverUri: Uri, origin: FileOrigin, parent: FileObject.Folder?) = withContext(Dispatchers.IO) {
        // Get file name
        val cursor = contentResolver.query(contentResolverUri, null, null, null, null)
            ?: throw FileNotFoundException("Unable to get name (0)")

        val fileName = cursor.use {
            if (it.moveToFirst()) {
                val x = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                it.getString(x)
            } else {
                throw FileNotFoundException("Unable to get name (1)")
            }
        }

        // Copy file to cache...we need a file for OkHttp or we need to load it into memory
        val file = File(context.cacheDir, UUID.randomUUID().toString())
        file.outputStream().use {
            contentResolver.openInputStream(contentResolverUri)?.copyTo(it) ?: throw FileNotFoundException("Unable to open input")
        }
        file.deleteOnExit()

        // Trigger upload
        val upload = Upload(
            origin = origin,
            parent = parent,
            name = fileName,
            size = file.length(),
            source = file,
            fileApi = octoPrintProvider.octoPrint().createFilesApi()
        )
        Timber.i("[${upload.id}] Starting upload of ${file.path} -> $origin:${parent?.path ?: "/"} ")
        activeUploads.add(upload)
        context.startService(Intent(context, UploadFilesService::class.java))
    }

    fun endUpload(uploadId: String) {
        Timber.i("[${uploadId}] Ending upload ")
        val endedUploads = activeUploads.filter { it.id == uploadId }
        endedUploads.forEach {
            it.updateProgress(1f)
            it.source.delete()
        }
        activeUploads.removeAll(endedUploads)
    }
}