package de.crysxd.octoapp.octoprint.models.connection

data class ConnectionResponse(
    val current: Connection,
    val options: ConnectionOptions
) {

    data class Connection(
        val state: String,
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

    data class PrinterProfile(
        val name: String,
        val id: String
    )


}