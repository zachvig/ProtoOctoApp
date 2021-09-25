package de.crysxd.octoapp.help.tutorials

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import de.crysxd.octoapp.base.data.models.YoutubePlaylist
import de.crysxd.octoapp.base.data.repository.TutorialsRepository
import de.crysxd.octoapp.base.data.repository.TutorialsRepository.Companion.PLAYLIST_ID
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

class TutorialsViewModel(
    private val tutorialsRepository: TutorialsRepository,
) : BaseViewModel() {

    private val mutableViewState = MutableLiveData<ViewState>(ViewState.Loading)
    val viewState = mutableViewState.distinctUntilChanged()

    init {
        reloadPlaylist()
    }

    fun reloadPlaylist() = viewModelScope.launch(coroutineExceptionHandler) {
        try {
            mutableViewState.postValue(ViewState.Loading)
            mutableViewState.postValue(
                ViewState.Data(
                    videos = tutorialsRepository.getTutorials(),
                    seenUpUntil = tutorialsRepository.getTutorialsSeenUpUntil()
                )
            )
            tutorialsRepository.markTutorialsSeen()
        } catch (e: Exception) {
            mutableViewState.postValue(ViewState.Error)
            Timber.e(e)
        }
    }

    fun createUri(playlistItem: YoutubePlaylist.PlaylistItem): Uri = Uri.parse(
        "https://www.youtube.com/watch?v=${playlistItem.contentDetails?.videoId}&list=$PLAYLIST_ID"
    )

    sealed class ViewState {
        object Loading : ViewState()
        object Error : ViewState()
        data class Data(val videos: List<YoutubePlaylist.PlaylistItem>, val seenUpUntil: Date) : ViewState()
    }
}
