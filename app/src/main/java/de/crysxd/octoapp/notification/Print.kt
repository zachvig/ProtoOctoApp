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
    val secsLeft: Long,
) {
    enum class State {
        Printing,
        Pausing,
        Paused,
        Cancelling,
    }

    enum class Source {
        Live,
        Remote,
    }
}