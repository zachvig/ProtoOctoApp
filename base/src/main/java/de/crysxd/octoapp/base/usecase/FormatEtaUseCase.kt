package de.crysxd.octoapp.base.usecase

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ext.format
import timber.log.Timber
import java.util.Date
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
            param.showLabel -> context.getString(R.string.eta_x, eta.format(useCompactFutureDate = param.useCompactDate))
            else -> eta.format(useCompactFutureDate = param.useCompactDate)
        }
    }

    data class Params(
        val secsLeft: Long,
        val useCompactDate: Boolean,
        val allowRelative: Boolean = true,
        val showLabel: Boolean = true,
    )
}