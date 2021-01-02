package de.crysxd.octoapp.base.usecase

import android.content.Context
import de.crysxd.octoapp.base.R
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FormatDurationUseCase @Inject constructor(
    private val context: Context
) : UseCase<Long, String>() {

    init {
        suppressLogging = true
    }

    override suspend fun doExecute(param: Long, timber: Timber.Tree): String {
        val hours = TimeUnit.SECONDS.toHours(param)
        val minutes = TimeUnit.SECONDS.toMinutes(param - TimeUnit.HOURS.toSeconds(hours))

        val stringRes = when {
            hours > 0 -> R.string.x_hours_y_mins
            minutes < 1 -> R.string.less_than_a_minute
            else -> R.string.x_mins
        }

        return context.getString(stringRes, hours, minutes)
    }
}