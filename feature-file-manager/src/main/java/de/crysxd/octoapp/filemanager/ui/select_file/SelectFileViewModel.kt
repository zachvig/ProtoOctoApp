package de.crysxd.octoapp.filemanager.ui.select_file

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.squareup.picasso.Picasso
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase.Params
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.ui.file_details.FileDetailsFragmentArgs
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit


class SelectFileViewModel(
    private val loadFilesUseCase: LoadFilesUseCase,
    private val octoPreferences: OctoPreferences,
    private val octoPrintProvider: OctoPrintProvider,
    val picasso: LiveData<Picasso?>,
) : BaseViewModel() {

    companion object {
        private const val HIDE_THUMBNAIL_HINT_FOR_DAYS = 21L
    }

    val fileOrigin = FileOrigin.Local
    private val filesMediator = MutableLiveData<UiState>()
    private var filesInitialised = false
    private var showThumbnailHint = false
    private var lastFolder: FileObject.Folder? = null

    init {
        viewModelScope.launch(coroutineExceptionHandler) {
            octoPrintProvider.eventFlow("select-file").collect {
                if (it is Event.MessageReceived && it.message is Message.EventMessage.UpdatedFiles) {
                    filesInitialised = false
                    if (filesMediator.hasActiveObservers()) {
                        reload()
                    }
                }
            }
        }
    }

    fun loadFiles(folder: FileObject.Folder?): LiveData<UiState> {
        if (!filesInitialised) {
            filesInitialised = true
            lastFolder = folder
            viewModelScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
                try {
                    val loadedFolder = loadFilesUseCase.execute(Params(fileOrigin, folder))
                    delay(1000)
                    // Check if we should show the thumbnail hint
                    // As soon as we determine we should hide it, persist that info so we remember that
                    // we e.g. saw a thumbnail in the root folder when showing a sub folder
                    val showThumbnailHint = !isAnyThumbnailPresent(loadedFolder) && !isHideThumbnailHint() && isAnyFilePresent(loadedFolder)
                    if (!showThumbnailHint) {
                        hideThumbnailHint()
                    }

                    Timber.i("Loaded ${loadedFolder.size} files")
                    filesMediator.postValue(UiState(false, loadedFolder, showThumbnailHint))
                } catch (e: Exception) {
                    Timber.e(e)
                    filesInitialised = false
                    filesMediator.postValue(UiState(true, emptyList(), false))
                }
            }
        }

        return filesMediator
    }

    fun reload() {
        filesInitialised = false
        loadFiles(lastFolder)
    }

    private suspend fun isHideThumbnailHint(): Boolean = withContext(Dispatchers.IO) {
        octoPreferences.hideThumbnailHintUntil.after(Date())
    }

    fun hideThumbnailHint() = viewModelScope.launch(coroutineExceptionHandler) {
        octoPreferences.hideThumbnailHintUntil = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(HIDE_THUMBNAIL_HINT_FOR_DAYS))

        filesMediator.value?.let {
            filesMediator.postValue(it.copy(showThumbnailHint = false))
        }

        showThumbnailHint = false
    }

    private fun isAnyFilePresent(files: List<FileObject>): Boolean = files.any {
        when (it) {
            is FileObject.Folder -> isAnyFilePresent(it.children ?: emptyList())
            is FileObject.File -> true
        }
    }

    private fun isAnyThumbnailPresent(files: List<FileObject>): Boolean = files.any {
        when (it) {
            is FileObject.Folder -> isAnyThumbnailPresent(it.children ?: emptyList())
            is FileObject.File -> !it.thumbnail.isNullOrBlank()
        }
    }

    fun selectFile(file: FileObject) = AppScope.launch(coroutineExceptionHandler) {
        when (file) {
            is FileObject.File -> {
                navContoller.navigate(R.id.action_show_file_details, FileDetailsFragmentArgs(file).toBundle())
            }
            is FileObject.Folder -> {
                navContoller.navigate(R.id.action_open_folder, SelectFileFragmentArgs(file, showThumbnailHint).toBundle())
            }
        }
    }

    data class UiState(val error: Boolean, val files: List<FileObject>, val showThumbnailHint: Boolean)
}
