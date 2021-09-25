package de.crysxd.octoapp.base.data.models

import java.util.Date

data class SerialCommunication(
    val content: String,
    val date: Date,
    val serverDate: Date?,
    val source: Source
) {

    sealed class Source {
        object OctoPrint : Source()
        object OctoAppInternal : Source()
        object User : Source()
    }
}