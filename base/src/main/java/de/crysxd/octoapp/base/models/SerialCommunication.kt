package de.crysxd.octoapp.base.models

import java.util.*

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