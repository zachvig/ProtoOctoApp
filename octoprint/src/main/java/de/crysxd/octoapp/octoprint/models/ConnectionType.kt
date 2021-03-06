package de.crysxd.octoapp.octoprint.models

sealed class ConnectionType {
    object Primary : ConnectionType()
    object Alternative : ConnectionType()
    object OctoEverywhere : ConnectionType()
}