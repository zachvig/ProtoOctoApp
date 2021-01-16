package de.crysxd.octoapp.base.ui.common.troubleshoot

sealed class TroubleShootingResult {
    data class Running(val step: Int, val totalSteps: Int, val status: String) : TroubleShootingResult()
    object Success : TroubleShootingResult()
    data class Failure(
        val title: String,
        val description: String,
        val suggestions: List<String>,
        val exception: Throwable? = null,
        val offerSupport: Boolean = false
    ) : TroubleShootingResult()
}