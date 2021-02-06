package de.crysxd.octoapp.base.ext

import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_ANALYSIS
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_AVERAGE
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_ESTIMATE
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_LINEAR
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_MIXED_ANALYSIS
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_MIXED_AVERAGE

val String.isHlsStreamUrl get() = endsWith(".m3u") || endsWith(".m3u8")

fun String?.asPrintTimeLeftOriginColor() = when (this) {
    ORIGIN_LINEAR -> R.color.analysis_bad
    ORIGIN_ANALYSIS, ORIGIN_MIXED_ANALYSIS -> R.color.analysis_normal
    ORIGIN_AVERAGE, ORIGIN_MIXED_AVERAGE, ORIGIN_ESTIMATE -> R.color.analysis_good
    else -> android.R.color.transparent
}