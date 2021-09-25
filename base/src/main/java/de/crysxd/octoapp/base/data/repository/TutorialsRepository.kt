package de.crysxd.octoapp.base.data.repository

import de.crysxd.octoapp.base.data.models.YoutubePlaylist
import de.crysxd.octoapp.base.data.source.RemoteTutorialsDataSource
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber
import java.util.Date

class TutorialsRepository(
    private val dataSource: RemoteTutorialsDataSource,
) {
    companion object {
        private const val MAX_AGE_FOR_NEW_MS = 30 * 24 * 60 * 60 * 1000L
        const val PLAYLIST_ID = "PL1fjlNqlUKnUuWwB0Jb3wf70wBcF3u-wJ"
    }

    private var cached: List<YoutubePlaylist.PlaylistItem>? = null
    private val octoPreferences get() = BaseInjector.get().octoPreferences()

    suspend fun getTutorials() = cached ?: let {
        val new = dataSource.get(PLAYLIST_ID)
        cached = new
        new
    }

    fun getNewTutorialsCount() = cached?.count {
        val oldestForNew = Date(System.currentTimeMillis() - MAX_AGE_FOR_NEW_MS)
        val seenUpUntil = getTutorialsSeenUpUntil()
        val uploadedAt = it.contentDetails?.videoPublishedAt ?: Date(0L)
        uploadedAt > oldestForNew && uploadedAt > seenUpUntil
    } ?: 0

    fun getTutorialsSeenUpUntil() = octoPreferences.tutorialsSeenAt ?: Date(1L)

    fun markTutorialsSeen() {
        Timber.i("Marking tutorials as seen")
        octoPreferences.tutorialsSeenAt = Date()
    }
}