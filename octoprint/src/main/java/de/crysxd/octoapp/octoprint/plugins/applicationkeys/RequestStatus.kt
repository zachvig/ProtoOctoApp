package de.crysxd.octoapp.octoprint.plugins.applicationkeys

sealed class RequestStatus {
    object DeniedOrTimedOut : RequestStatus()
    object Pending : RequestStatus()
    data class Granted(val apiKey: String) : RequestStatus()
}