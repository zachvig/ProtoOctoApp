package de.crysxd.octoapp.base.data.models

import java.util.Date

data class YoutubePlaylist(
    val items: List<PlaylistItem>?
) {
    data class PlaylistItem(
        val snippet: Snippet?,
        val contentDetails: ContentDetails?,
    ) {
        companion object {
            private const val MAX_AGE_FOR_NEW_MS = 30 * 24 * 60 * 60 * 1000L
        }

        fun isNew(seenUpUntil: Date): Boolean {
            val oldestForNew = Date(System.currentTimeMillis() - MAX_AGE_FOR_NEW_MS)
            val uploadedAt = contentDetails?.videoPublishedAt ?: Date(0L)
            return uploadedAt > oldestForNew && uploadedAt > seenUpUntil
        }

        data class Snippet(
            val title: String?,
            val description: String?,
            val thumbnails: Map<String, Thumbnail>?,
        )

        data class Thumbnail(
            val url: String?,
            val width: Int,
            val height: Int,
        )

        data class ContentDetails(
            val videoId: String?,
            val videoPublishedAt: Date?,
        )
    }
}