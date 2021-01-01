package de.crysxd.octoapp.base.utils

import android.content.Context
import androidx.annotation.PluralsRes
import de.crysxd.octoapp.base.R
import timber.log.Timber
import java.util.concurrent.TimeUnit

sealed class LongDuration(open val value: Int, @PluralsRes private val label: Int) {
    companion object {
        fun parse(text: String): LongDuration? = try {
            if (text.isBlank()) {
                null
            } else {
                val value = text.substring(1, text.length - 1).toInt()
                when (text.last()) {
                    'D' -> Days(value = value)
                    'W' -> Weeks(value = value)
                    'M' -> Months(value = value)
                    'Y' -> Years(value = value)
                    else -> null
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    fun inSeconds() = when (this) {
        is Days -> TimeUnit.DAYS.toSeconds(value.toLong())
        is Weeks -> TimeUnit.DAYS.toSeconds(value.toLong() * 7)
        is Months -> TimeUnit.DAYS.toSeconds(value.toLong() * 28)
        is Years -> TimeUnit.DAYS.toSeconds(value.toLong() * 365)
    }

    fun format(context: Context) = context.resources.getQuantityString(label, value, value)

    data class Days(override val value: Int) : LongDuration(value, R.plurals.x_days)
    data class Weeks(override val value: Int) : LongDuration(value, R.plurals.x_weeks)
    data class Months(override val value: Int) : LongDuration(value, R.plurals.x_months)
    data class Years(override val value: Int) : LongDuration(value, R.plurals.x_years)
}