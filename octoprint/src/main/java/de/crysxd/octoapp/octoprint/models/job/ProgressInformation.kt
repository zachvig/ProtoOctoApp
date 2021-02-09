package de.crysxd.octoapp.octoprint.models.job

data class ProgressInformation(
    val completion: Float,
    val filepos: Long,
    val printTime: Int,
    val printTimeLeft: Int,
    val printTimeLeftOrigin: String?
) {

    companion object {
        const val ORIGIN_LINEAR = "linear"
        const val ORIGIN_ANALYSIS = "analysis"
        const val ORIGIN_ESTIMATE = "estimate"
        const val ORIGIN_AVERAGE = "average"
        const val ORIGIN_MIXED_ANALYSIS = "mixed-analysis"
        const val ORIGIN_MIXED_AVERAGE = "mixed-average"
    }
}