package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.squareup.picasso.Picasso
import de.crysxd.octoapp.base.OctoPrintProvider
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
import java.util.*
import java.util.concurrent.TimeUnit

const val KEY_HIDE_THUMBNAIL_HINT_UNTIL = "hide_thumbnail_hin_until"
const val HIDE_THUMBNAIL_HINT_FOR_DAYS = 21L

class SelectFileViewModel(
    private val octoPrintProvider: OctoPrintProvider,
    private val loadFilesUseCase: LoadFilesUseCase,
    private val startPrintJobUseCase: StartPrintJobUseCase,
    private val sharedPreferences: SharedPreferences,
    val picasso: LiveData<Picasso?>
) : BaseViewModel() {

    private val rootFilesMediator = MediatorLiveData<UiState>()
    private var rootFilesInitialised = false
    private var showThumbnailHint = false

    fun loadRootFiles(): LiveData<UiState> {
        if (!rootFilesInitialised) {
            rootFilesInitialised = true
            rootFilesMediator.addSource(octoPrintProvider.octoPrint) {
                viewModelScope.launch(coroutineExceptionHandler) {
                    it?.let {
                        val root = loadFilesUseCase.execute(Params(it, FileOrigin.Local))
                        showThumbnailHint = !isAnyThumbnailPresent(root) && !isHideThumbnailHint()
                        rootFilesMediator.postValue(UiState(root, showThumbnailHint))
                    }
                }
            }
        }

        return Transformations.map(rootFilesMediator) { it }
    }

    private suspend fun isHideThumbnailHint(): Boolean = withContext(Dispatchers.IO) {
        Date(sharedPreferences.getLong(KEY_HIDE_THUMBNAIL_HINT_UNTIL, 0)).after(Date())
    }

    fun hideThumbnailHint() = viewModelScope.launch(coroutineExceptionHandler) {
        sharedPreferences.edit {
            putLong(KEY_HIDE_THUMBNAIL_HINT_UNTIL, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(HIDE_THUMBNAIL_HINT_FOR_DAYS))
        }

        rootFilesMediator.value?.let {
            rootFilesMediator.postValue(it.copy(showThumbnailHint = false))
        }

        showThumbnailHint = false
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
                octoPrintProvider.octoPrint.value?.let {
                    startPrintJobUseCase.execute(Pair(it, file))
                }
            }
            is FileObject.Folder -> {
                navContoller.navigate(R.id.action_open_folder, SelectFileFragmentArgs(file, showThumbnailHint).toBundle())
            }
        }
    }

    data class UiState(val files: List<FileObject>, val showThumbnailHint: Boolean)
}
