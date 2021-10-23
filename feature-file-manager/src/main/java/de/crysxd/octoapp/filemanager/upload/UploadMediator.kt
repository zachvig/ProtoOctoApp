package de.crysxd.octoapp.filemanager.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import de.crysxd.octoapp.base.data.models.exceptions.SuppressedException
import de.crysxd.octoapp.base.data.models.exceptions.UserMessageException
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.filemanager.di.FileManagerScope
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import javax.inject.Inject


@FileManagerScope
class UploadMediator @Inject constructor(
    private val context: Context,
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
) {

    private val contentResolver = context.contentResolver
    private val activeUploadsFlow = MutableStateFlow<List<Upload>>(emptyList())
    private val mutex = Mutex()

    fun getActiveUploads() = activeUploadsFlow.value.toList()

    fun getActiveUploads(origin: FileOrigin, parent: FileObject.Folder?) = activeUploadsFlow.map { list ->
        list.filter {
            it.parent?.path == parent?.path && origin == it.origin
        }
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

        // Check if valid file
        checkValidFile(fileName)

        // Copy file to cache...we need a file for OkHttp or we need to load it into memory
        val file = File(BaseInjector.get().publicFileDirectory(), UUID.randomUUID().toString())
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

        mutex.withLock {
            activeUploadsFlow.value = activeUploadsFlow.value.toMutableList().also {
                it.add(upload)
            }
        }

        context.startService(Intent(context, UploadFilesService::class.java))
    }

    private fun checkValidFile(name: String) {
        // OctoPrint support g, gco and gcode files out of the box
        // With the Ultimaker Format Plugin it can also handle upf files, but there is no way to check
        // if this plugin is installed (reqquires plugin manager permission), so we just assume it's ok and
        // let the upload fail on the server side
        val extension = name.split(".").last()
        val extensions = mutableListOf("g", "gco", "gcode", "upf")

        // If we have the UploadAnything plugin installed, add the allowed extensions from there as well
        val settings = octoPrintRepository.getActiveInstanceSnapshot()?.settings
        settings?.plugins?.values?.mapNotNull { it as? Settings.UploadAnything }?.firstOrNull()?.allowedExtensions?.let {
            extensions.addAll(it)
        }

        // Check if we can upload the file
        if (!extensions.contains(extension.lowercase())) {
            throw UnsupportedFileException(extensions)
        }
    }

    suspend fun endUpload(uploadId: String) = mutex.withLock {
        Timber.i("[${uploadId}] Ending upload ")
        val endedUploads = getActiveUploads().filter { it.id == uploadId }.onEach {
            it.updateProgress(1f)
            it.source.delete()
        }

        activeUploadsFlow.value = activeUploadsFlow.value.toMutableList().also {
            it.removeAll(endedUploads)
        }
    }

    class UnsupportedFileException(private val allowedExtensions: List<String>) : IllegalArgumentException(), UserMessageException, SuppressedException {
        override fun getUserMessage(context: Context) =
            "This file type is not supported by OctoPrint.**<br><br><small>Supported file types: ${allowedExtensions.joinToString()}</small>"
    }
}