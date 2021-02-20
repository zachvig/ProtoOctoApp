package de.crysxd.octoapp.base.usecase

import android.content.Context
import de.crysxd.octoapp.base.R
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FormatEtaUseCase @Inject constructor(
    private val formatDurationUseCase: FormatDurationUseCase,
    private val context: Context
) : UseCase<FormatEtaUseCase.Params, String>() {

    init {
        suppressLogging = true
    }

    override suspend fun doExecute(param: Params, timber: Timber.Tree): String {
        val eta = Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(param.secsLeft))
        val lessThanAnHour = TimeUnit.SECONDS.toMinutes(param.secsLeft) < 60
        return when {
            lessThanAnHour && param.allowRelative -> context.getString(R.string.x_left, formatDurationUseCase.execute(param.secsLeft))
            eta.isToday() -> context.getString(R.string.eta_x, DateFormat.getTimeInstance(DateFormat.SHORT).format(eta))
            else -> context.getString(R.string.eta_x, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(eta))
        }
    }

    private fun Date.isToday(): Boolean {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        return format.format(Date()) == format.format(this)
    }

    data class Params(
        val secsLeft: Long,
        val allowRelative: Boolean
    )
}