package de.crysxd.octoapp.filemanager.ui.select_file

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.squareup.picasso.Picasso
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.FileManagerSettings
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.LoadFileUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase.Params
import de.crysxd.octoapp.base.usecase.MoveFileUseCase
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.ui.file_details.FileDetailsFragmentArgs
import de.crysxd.octoapp.filemanager.upload.Upload
import de.crysxd.octoapp.filemanager.upload.UploadMediator
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

class SelectFileViewModel(
    private val loadFilesUseCase: LoadFilesUseCase,
    private val loadFileUseCase: LoadFileUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val octoPreferences: OctoPreferences,
    private val octoPrintProvider: OctoPrintProvider,
    private val uploadMediator: UploadMediator,
    val picasso: LiveData<Picasso?>,
) : BaseViewModel() {

    companion object {
        private const val HIDE_THUMBNAIL_HINT_FOR_DAYS = 21L
    }

    val fileOrigin = FileOrigin.Local
    private val showThumbnailFlow = MutableStateFlow(true)
    private var lastFolder = MutableStateFlow<FileObject.Folder?>(null)
    private var lastFileList: List<FileObject>? = null
    private var lastSelectedFile: FileObject.File? = null
    private val trigger = MutableStateFlow<Int?>(null)
    private val uploadsFlow = lastFolder.flatMapLatest {
        uploadMediator.getActiveUploads(fileOrigin, it)
    }
    val uiState = trigger.filterNotNull().combine(lastFolder) { _, folder ->
        folder
    }.flatMapLatest {
        Timber.i("Loading ${it?.path ?: "/"} ")
        flow {
            val files = if (lastFileList == null) {
                val files = loadFilesUseCase.execute(Params(fileOrigin, it))

                // Check if the user already uses thumbnails
                if (isAnyThumbnailPresent(files)) {
                    showThumbnailFlow.value = false
                }
                lastFileList = files
                files
            } else {
                lastFileList!! // Will throw and retry (like worst case)
            }

            emit(files)
        }
    }.combine(getSelectFile()) { allFiles, selectedFile ->
        Timber.i("Selected file received: ${selectedFile?.path}")
        allFiles to selectedFile
    }.combine(octoPreferences.updatedFlow) { allFilesAndSelected, _ ->
        allFilesAndSelected
    }.combine(uploadsFlow) { allFilesAndSelected, uploads ->
        val (allFiles, selectedFile) = allFilesAndSelected
        val selected = listOfNotNull(
            selectedFile?.let { FileWrapper.SelectedFileObjectWrapper(it) }
        )

        val folders = allFiles.filterIsInstance<FileObject.Folder>()
            .map { FileWrapper.FileObjectWrapper(it) }
            .sortedBy { it.fileObject.display?.lowercase() }

        val files = listOf(
            allFiles.filterIsInstance<FileObject.File>().map { FileWrapper.FileObjectWrapper(it) },
            uploads.map { FileWrapper.UploadWrapper(it) }
        ).flatten().sorted().filter {
            it.path != selectedFile?.path
        }

        listOf(
            selected,
            folders,
            files
        ).flatten()
    }.combine(showThumbnailFlow) { files, showThumbnail ->
        UiState.DataReady(files, showThumbnail) as UiState
    }.retry(1).catch {
        Timber.e(it)
    }.catch {
        Timber.e(it)
        emit(UiState.Error(it))
    }

    init {
        viewModelScope.launch(coroutineExceptionHandler) {
            octoPrintProvider.eventFlow("select-file").collect {
                if (it is Event.MessageReceived && it.message is Message.EventMessage.UpdatedFiles) {
                    reload()
                }
            }
        }
    }

    fun loadFiles(folder: FileObject.Folder?) = viewModelScope.launch(coroutineExceptionHandler) {
        if (lastFileList == null || lastFolder.value?.path != folder?.path) {
            lastFolder.value = folder
            showThumbnailFlow.value = folder == null && !isHideThumbnailHint()
            reload()
        }
    }

    fun reload() = viewModelScope.launch(coroutineExceptionHandler) {
        lastFileList = null
        lastSelectedFile = null
        trigger.value = (trigger.value ?: 0) + 1
    }

    private fun List<FileWrapper>.sorted() = octoPreferences.fileManagerSettings.let {
        val comparator = when (it.sortBy) {
            FileManagerSettings.SortBy.UploadTime -> compareBy<FileWrapper> { it.date }
            FileManagerSettings.SortBy.PrintTime -> compareBy { it.lastPrintDate }
            FileManagerSettings.SortBy.FileSize -> compareBy { it.size }
            FileManagerSettings.SortBy.Name -> compareBy { it.name }
        }.thenBy { it.path }

        val sorted = this.filter { f -> !f.wasPrinted || !it.hidePrintedFiles }.sortedWith(comparator)

        when (it.sortDirection) {
            FileManagerSettings.SortDirection.Ascending -> sorted
            FileManagerSettings.SortDirection.Descending -> sorted.reversed()
        }
    }

    private fun getSelectFile() = octoPrintProvider.passiveCurrentMessageFlow("list-files").map {
        it.job?.file
    }.combine(lastFolder) { selectedFile, folder ->
        folder to selectedFile
    }.distinctUntilChangedBy {
        it.second?.path
    }.map {
        // Only load selected file when the folder is null (root dir)
        it.second.takeIf { _ -> it.first == null }
    }.flatMapLatest {
        it?.let {
            Timber.i("Selected file updated: ${it.path}")
            flow {
                if (lastSelectedFile?.path == it.path) {
                    emit(lastSelectedFile)
                } else {
                    val path = it.path ?: return@flow
                    lastSelectedFile = loadFileUseCase.execute(LoadFileUseCase.Params(fileOrigin, path))
                    Timber.i("Downloaded details for selected file: $lastSelectedFile")
                    emit(lastSelectedFile)
                }
            }
        } ?: flowOf(null)
    }.catch {
        Timber.e(it)
        emit(null)
    }

    private fun isHideThumbnailHint() = octoPreferences.hideThumbnailHintUntil.after(Date())

    fun hideThumbnailHint() {
        octoPreferences.hideThumbnailHintUntil = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(HIDE_THUMBNAIL_HINT_FOR_DAYS))
        showThumbnailFlow.value = false
    }

    private fun isAnyThumbnailPresent(files: List<FileObject>): Boolean = files.any {
        when (it) {
            is FileObject.Folder -> isAnyThumbnailPresent(it.children ?: emptyList())
            is FileObject.File -> !it.thumbnail.isNullOrBlank()
        }
    }

    fun selectFile(file: FileObject) = viewModelScope.launch(coroutineExceptionHandler) {
        when (file) {
            is FileObject.File -> navContoller.navigate(R.id.action_show_file_details, FileDetailsFragmentArgs(file).toBundle())
            is FileObject.Folder -> navContoller.navigate(R.id.action_open_folder, SelectFileFragmentArgs(file).toBundle())
        }
    }

    fun moveFileHere(fileObject: FileObject, copyFile: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        moveFileUseCase.execute(MoveFileUseCase.Params(file = fileObject, newPath = lastFolder.value?.path ?: "/", copyFile = copyFile))
    }

    sealed class UiState {
        open class Loading : UiState()
        object Initial : Loading()
        data class Error(val exception: Throwable) : UiState()
        data class DataReady(val files: List<FileWrapper>, val showThumbnailHint: Boolean) : UiState()
    }

    sealed class FileWrapper {
        abstract val date: Long
        abstract val path: String?
        abstract val name: String?
        abstract val size: Long?
        abstract val lastPrintDate: Long
        abstract val wasPrinted: Boolean

        data class UploadWrapper(val upload: Upload) : FileWrapper() {
            override val date = upload.startTime.time
            override val path = upload.parent?.path + upload.name
            override val name = upload.name
            override val size = upload.size
            override val lastPrintDate = 0L
            override val wasPrinted = false
        }

        data class SelectedFileObjectWrapper(val fileObject: FileObject.File) : FileWrapper() {
            override val date = fileObject.date * 1000
            override val path = fileObject.path
            override val name = fileObject.name
            override val size = fileObject.size
            override val lastPrintDate = ((fileObject.prints?.last?.date ?: 0f) * 1000).toLong()
            override val wasPrinted = false
        }

        data class FileObjectWrapper(val fileObject: FileObject) : FileWrapper() {
            override val path = fileObject.path
            override val name = fileObject.name
            override val size = fileObject.size
            override val lastPrintDate = (((fileObject as? FileObject.File)?.prints?.last?.date ?: 0f) * 1000).toLong()
            override val wasPrinted = (fileObject as? FileObject.File)?.prints?.last?.success == true
            override val date = when (fileObject) {
                is FileObject.File -> fileObject.date * 1000
                is FileObject.Folder -> Long.MAX_VALUE
            }
        }
    }
}
