package de.crysxd.octoapp.help.tutorials

import java.util.Date

data class YoutubePlaylist(
    val items: List<PlaylistItem>?
) {
    data class PlaylistItem(
        val snippet: Snippet?,
        val contentDetails: ContentDetails?,
    ) {
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