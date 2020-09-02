package de.crysxd.octoapp.octoprint.models.connection

data class ConnectionResponse(
    val current: Connection,
    val options: ConnectionOptions
) {

    data class Connection(
        val state: ConnectionState,
        val port: String,
        val baudrate: Int,
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

    enum class ConnectionState {
        CLOSED,
        DETECTING_SERIAL_PORT,
        DETECTING_BAUDRATE,
        CONNECTING,
        OPERATIONAL,
        ERROR_FAILED_TO_AUTODETECT_SERIAL_PORT,
        CONNECTION_ERROR,
        DETECTING_SERIAL_CONNECTION,
        PRINTING,
        CANCELLING,
        FINISHING,
        UNKNOWN_ERROR,
        UNKNOWN,
    }

    data class PrinterProfile(
        val name: String,
        val id: String
    )


}