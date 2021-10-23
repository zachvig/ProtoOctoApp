package de.crysxd.octoapp.filemanager.upload

import de.crysxd.octoapp.octoprint.api.FilesApi
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Date
import java.util.UUID

data class Upload(
    val id: String = UUID.randomUUID().toString(),
    val fileApi: FilesApi.Wrapper,
    val origin: FileOrigin,
    val parent: FileObject.Folder?,
    val name: String,
    val source: File,
    val size: Long,
    val startTime: Date = Date()
) {
    private val progressFlow = MutableStateFlow(0f)
    val progress = progressFlow.asStateFlow()

    fun updateProgress(progress: Float) {
        progressFlow.value = progress
    }
}