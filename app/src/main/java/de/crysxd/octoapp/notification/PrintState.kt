package de.crysxd.octoapp.notification

import java.util.Date

data class PrintState(
    val fileDate: Long,
    val fileName: String,
    val source: Source,
    val state: State,
    val progress: Float,
    val sourceTime: Date,
    val appTime: Date,
    val eta: Date?,
) {

    companion object {
        const val DEFAULT_FILE_TIME = 0L
        const val DEFAULT_FILE_NAME = "unknown"
        const val DEFAULT_PROGRESS = 0f
    }

    val objectId get() = "$fileDate$fileName"

    enum class State {
        Printing,
        Pausing,
        Paused,
        Idle,
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