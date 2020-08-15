package de.crysxd.octoapp.base.usecase

import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FormatDurationUseCase @Inject constructor() : UseCase<Long, String>() {

    init {
        suppressLogging = true
    }

    override suspend fun doExecute(param: Long, timber: Timber.Tree): String {
        val hours = TimeUnit.SECONDS.toHours(param)
        val minutes = TimeUnit.SECONDS.toMinutes(param - TimeUnit.HOURS.toSeconds(hours))

        val format = when {
            hours > 0 -> "%1$02d:%2$02d h"
            minutes < 1 -> "less than a minute"
            else -> "%2\$d min"
        }

        return String.format(format, hours, minutes)
    }
}