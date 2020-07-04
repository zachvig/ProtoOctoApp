package de.crysxd.octoapp.octoprint.models.connection

sealed class ConnectionCommand(val command: String) {

    data class Connect(
        val port: String? = null,
        val baudrate: Int? = null,
        val printerProfile: String? = null,
        val save: Boolean? = null,
        val autoconnect: Boolean? = null
    ) : ConnectionCommand("connect")

    object Disconnect : ConnectionCommand("disconnect")

    object FakeAck : ConnectionCommand("fake_ack")

}