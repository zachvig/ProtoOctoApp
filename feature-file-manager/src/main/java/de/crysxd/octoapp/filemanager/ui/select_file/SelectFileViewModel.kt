package de.crysxd.octoapp.filemanager.ui.select_file

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.squareup.picasso.Picasso
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase.Params
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.ui.file_details.FileDetailsFragmentArgs
import de.crysxd.octoapp.filemanager.upload.Upload
import de.crysxd.octoapp.filemanager.upload.UploadMediator
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit


class SelectFileViewModel(
    private val loadFilesUseCase: LoadFilesUseCase,
    private val octoPreferences: OctoPreferences,
    private val octoPrintProvider: OctoPrintProvider,
    private val uploadMediator: UploadMediator,
    val picasso: LiveData<Picasso?>,
) : BaseViewModel() {

    companion object {
        private const val HIDE_THUMBNAIL_HINT_FOR_DAYS = 21L
    }

    val fileOrigin = FileOrigin.Local
    private val mutableUiState = MutableStateFlow<Flow<UiState>>(flowOf(UiState.Initial))
    private val showThumbnailFlow = MutableStateFlow(false)
    private var lastFolder: FileObject.Folder? = null
    private var lastFileList: List<FileObject>? = null
    val uiState = mutableUiState.flatMapLatest { it }

    init {
        viewModelScope.launch(coroutineExceptionHandler) {
            octoPrintProvider.eventFlow("select-file").collect {
                if (it is Event.MessageReceived && it.message is Message.EventMessage.UpdatedFiles) {
                    if (mutableUiState.subscriptionCount.first() > 0) {
                        loadFiles(folder = lastFolder, reload = true)
                    }
                }
            }
        }
    }

    fun setupThumbnailHint(showThumbnailHint: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        showThumbnailFlow.value = !isHideThumbnailHint() && showThumbnailHint
    }

    fun loadFiles(folder: FileObject.Folder?, reload: Boolean = false) = viewModelScope.launch(coroutineExceptionHandler) {
        if (reload || uiState.first() == UiState.Initial) {
            lastFolder = folder

            mutableUiState.value = flow {
                lastFileList?.let { emit(it) }

                val files = loadFilesUseCase.execute(Params(fileOrigin, folder))

                // Check if the user already uses thumbnails
                if (isAnyThumbnailPresent(files)) {
                    showThumbnailFlow.value = false
                }

                lastFileList = files
                emit(files)
            }.combine(uploadMediator.getActiveUploads(fileOrigin, lastFolder)) { allFiles, uploads ->
                val folders = allFiles.filterIsInstance<FileObject.Folder>()
                    .map { FileWrapper.FileObjectWrapper(it) }
                    .sortedBy { it.fileObject.display.lowercase() }

                val files = listOf(
                    allFiles.filterIsInstance<FileObject.File>().map { FileWrapper.FileObjectWrapper(it) },
                    uploads.map { FileWrapper.UploadWrapper(it) }
                ).flatten().sortedByDescending { it.date }

                listOf(folders, files).flatten()
            }.combine(showThumbnailFlow) { files, showThumbnail ->
                UiState.DataReady(files, showThumbnail)
            }.retry(1).catch {
                Timber.e(it)
                UiState.Error(it)
            }
        }
    }

    private suspend fun isHideThumbnailHint(): Boolean = withContext(Dispatchers.IO) {
        octoPreferences.hideThumbnailHintUntil.after(Date())
    }

    fun hideThumbnailHint() = viewModelScope.launch(coroutineExceptionHandler) {
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
            is FileObject.Folder -> navContoller.navigate(R.id.action_open_folder, SelectFileFragmentArgs(file, showThumbnailFlow.value).toBundle())
        }
    }

    sealed class UiState {
        open class Loading : UiState()
        object Initial : Loading()
        data class Error(val exception: Throwable) : UiState()
        data class DataReady(val files: List<FileWrapper>, val showThumbnailHint: Boolean) : UiState()
    }

    sealed class FileWrapper {
        abstract val date: Date

        data class UploadWrapper(val upload: Upload) : FileWrapper() {
            override val date = upload.startTime
        }

        data class FileObjectWrapper(val fileObject: FileObject) : FileWrapper() {
            override val date = when (fileObject) {
                is FileObject.File -> Date(fileObject.date)
                is FileObject.Folder -> Date(Long.MAX_VALUE)
            }
        }
    }
}
