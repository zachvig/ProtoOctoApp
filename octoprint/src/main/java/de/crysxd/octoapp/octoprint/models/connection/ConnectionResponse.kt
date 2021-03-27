package de.crysxd.octoapp.octoprint.models.connection

import com.google.gson.annotations.SerializedName

data class ConnectionResponse(
    val current: Connection,
    val options: ConnectionOptions
) {

    data class Connection(
        val state: ConnectionState,
        val port: String?,
        val baudrate: Int?,
        val printerProfile: String
    )

    data class ConnectionOptions(
        val ports: List<String>,
        val baudrates: List<Int>,
        val printerProfiles: List<PrinterProfile>,
        val portPreference: String,
        val baudratePreference: Int,
        val printerProfilePreference: String,
        val autoConnect: Boolean
    )

    // This field is derived from a UI string and is not reliable!
    // This should only be used for UI states etc but not for important decisions
    enum class ConnectionState {
        MAYBE_DETECTING_SERIAL_PORT,
        MAYBE_DETECTING_BAUDRATE,
        MAYBE_CONNECTING,
        MAYBE_OPERATIONAL,
        MAYBE_CLOSED,
        MAYBE_ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT,
        MAYBE_CONNECTION_ERROR,
        MAYBE_DETECTING_SERIAL_CONNECTION,
        MAYBE_PRINTING,
        MAYBE_UNKNOWN_ERROR,
        OTHER,
    }

    data class PrinterProfile(
        val id: String,
        val model: String?,
        val name: String?,
    )
}