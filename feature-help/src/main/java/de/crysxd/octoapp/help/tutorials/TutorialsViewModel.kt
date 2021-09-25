package de.crysxd.octoapp.help.tutorials

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.Date

class TutorialsViewModel : BaseViewModel() {

    companion object {
        private const val PLAYLIST_ID = "PL1fjlNqlUKnUuWwB0Jb3wf70wBcF3u-wJ"
    }

    private val mutableViewState = MutableLiveData<ViewState>(ViewState.Loading)
    val viewState = mutableViewState.distinctUntilChanged()

    init {
        reloadPlaylist()
    }

    fun reloadPlaylist() = viewModelScope.launch(coroutineExceptionHandler) {
        try {
            mutableViewState.postValue(ViewState.Loading)
            val playlist = Retrofit.Builder()
                .client(OkHttpClient.Builder().build())
                .baseUrl("https://youtube.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YoutubeApi::class.java)
                .getPlaylist(PLAYLIST_ID)
            require(!playlist.items.isNullOrEmpty()) { "Playlist is empty" }
            val items = playlist.items.sortedByDescending {
                it.contentDetails?.videoPublishedAt ?: Date(0)
            }.filter {
                // Private videos have no publishing date
                it.contentDetails?.videoPublishedAt != null
            }
            mutableViewState.postValue(ViewState.Data(items))
        } catch (e: Exception) {
            mutableViewState.postValue(ViewState.Error)
            Timber.e(e)
        }
    }

    fun createUri(playlistItem: YoutubePlaylist.PlaylistItem) = Uri.parse(
        "https://www.youtube.com/watch?v=${playlistItem.contentDetails?.videoId}&list=$PLAYLIST_ID"
    )

    sealed class ViewState {
        object Loading : ViewState()
        object Error : ViewState()
        data class Data(val videos: List<YoutubePlaylist.PlaylistItem>) : ViewState()
    }
}
