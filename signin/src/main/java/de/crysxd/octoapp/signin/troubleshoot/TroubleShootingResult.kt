package de.crysxd.octoapp.signin.troubleshoot

sealed class TroubleShootingResult {
    data class Running(val status: CharSequence) : TroubleShootingResult()
    object Success : TroubleShootingResult()
    data class Failure(val text: CharSequence, val suggestions: List<CharSequence>, val offerSupport: Boolean = false) : TroubleShootingResult()
}