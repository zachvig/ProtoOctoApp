package de.crysxd.octoapp.base.ext

import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_ANALYSIS
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_AVERAGE
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_ESTIMATE
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_GENIUS
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_LINEAR
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_MIXED_ANALYSIS
import de.crysxd.octoapp.octoprint.models.job.ProgressInformation.Companion.ORIGIN_MIXED_AVERAGE
import java.net.URLDecoder
import java.net.URLEncoder

val String.isHlsStreamUrl get() = endsWith(".m3u") || endsWith(".m3u8")

fun String?.asPrintTimeLeftOriginColor() = when (this) {
    ORIGIN_LINEAR -> R.color.analysis_bad
    ORIGIN_ANALYSIS, ORIGIN_MIXED_ANALYSIS -> R.color.analysis_normal
    ORIGIN_AVERAGE, ORIGIN_MIXED_AVERAGE, ORIGIN_ESTIMATE -> R.color.analysis_good
    ORIGIN_GENIUS -> R.color.yellow
    else -> android.R.color.transparent
}

fun String?.asPrintTimeLeftImageResource() = when (this) {
    ORIGIN_GENIUS -> R.drawable.ic_round_star_18
    else -> R.drawable.eta_ball
}

fun String?.urlEncode() = URLEncoder.encode(this, "UTF-8")
fun String?.urlDecode() = URLDecoder.decode(this, "UTF-8")
fun String.toHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT)