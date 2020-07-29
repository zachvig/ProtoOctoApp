package de.crysxd.octoapp.base.usecase

import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FormatDurationUseCase @Inject constructor() : UseCase<Long, String> {

    override suspend fun execute(param: Long): String {
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