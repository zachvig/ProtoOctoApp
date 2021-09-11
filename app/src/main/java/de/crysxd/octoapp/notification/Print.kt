package de.crysxd.octoapp.notification

import java.util.Date

data class Print(
    val objectId: String,
    val fileName: String,
    val source: Source,
    val state: State,
    val progress: Float,
    val sourceTime: Date,
    val appTime: Date,
    val eta: Date?,
) {
    enum class State {
        Printing,
        Pausing,
        Paused,
        Cancelling,
    }

    enum class Source {
        Live,
        CachedLive,
        Remote,
        CachedRemote;

        val asCached
            get() = when (this) {
                Live, CachedLive -> CachedLive
                Remote, CachedRemote -> CachedRemote
            }
    }
}