package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase.Params
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import de.crysxd.octoapp.pre_print_controls.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

const val KEY_HIDE_THUMBNAIL_HINT_UNTIL = "hide_thumbnail_hin_until"
const val HIDE_THUMBNAIL_HINT_FOR_DAYS = 21L

class SelectFileViewModel(
    private val loadFilesUseCase: LoadFilesUseCase,
    private val startPrintJobUseCase: StartPrintJobUseCase,
    private val sharedPreferences: SharedPreferences,
    val picasso: LiveData<Picasso?>
) : BaseViewModel() {

    private val filesMediator = MediatorLiveData<UiState>()
    private var filesInitialised = false
    private var showThumbnailHint = false
    private var lastFolder: FileObject.Folder? = null

    fun loadFiles(folder: FileObject.Folder?): LiveData<UiState> {
        if (!filesInitialised) {
            filesInitialised = true
            lastFolder = folder
            viewModelScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
                try {
                    val loadedFolder = loadFilesUseCase.execute(Params(FileOrigin.Local, folder))

                    // Check if we should show the thumbnail hint
                    // As soon as we determine we should hide it, persist that info so we remember that
                    // we e.g. saw a thumbnail in the root folder when showing a sub folder
                    val showThumbnailHint = !isAnyThumbnailPresent(loadedFolder) && !isHideThumbnailHint() && isAnyFilePresent(loadedFolder)
                    if (!showThumbnailHint) {
                        hideThumbnailHint()
                    }

                    filesMediator.postValue(UiState(false, loadedFolder, showThumbnailHint))
                } catch (e: Exception) {
                    Timber.e(e)
                    filesMediator.postValue(UiState(true, emptyList(), false))
                }
            }
        }

        return Transformations.map(filesMediator) { it }
    }

    fun reload() {
        filesInitialised = false
        loadFiles(lastFolder)
    }

    private suspend fun isHideThumbnailHint(): Boolean = withContext(Dispatchers.IO) {
        Date(sharedPreferences.getLong(KEY_HIDE_THUMBNAIL_HINT_UNTIL, 0)).after(Date())
    }

    fun hideThumbnailHint() = viewModelScope.launch(coroutineExceptionHandler) {
        sharedPreferences.edit {
            putLong(KEY_HIDE_THUMBNAIL_HINT_UNTIL, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(HIDE_THUMBNAIL_HINT_FOR_DAYS))
        }

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

    fun selectFile(file: FileObject) = GlobalScope.launch(coroutineExceptionHandler) {
        when (file) {
            is FileObject.File -> {
                startPrintJobUseCase.execute(file)
            }
            is FileObject.Folder -> {
                navContoller.navigate(R.id.action_open_folder, SelectFileFragmentArgs(file, showThumbnailHint).toBundle())
            }
        }
    }

    data class UiState(val error: Boolean, val files: List<FileObject>, val showThumbnailHint: Boolean)
}
