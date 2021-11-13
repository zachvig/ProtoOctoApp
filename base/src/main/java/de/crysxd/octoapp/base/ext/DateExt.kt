package de.crysxd.octoapp.base.ext

import android.content.res.Resources
import android.text.format.DateUtils
import androidx.core.os.ConfigurationCompat
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val dateFlags =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME

// We can't use Locale.getDefault() because we overwrite the app language but we _always_ want to use the user's local preferences for time
private fun getDeviceLocale() = try {
    val deviceLanguage = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
    Locale.forLanguageTag(deviceLanguage)
} catch (e: Exception) {
    // Just precautionary try/catch
    Timber.e(e)
    Locale.getDefault()
}

fun Date.format(forceShowDate: Boolean = false, useCompactFutureDate: Boolean = false): String = when {
    // Today? Only format time
    isToday() && !forceShowDate -> DateFormat.getTimeInstance(DateFormat.SHORT, getDeviceLocale()).format(this)

    // Is in future? Use a "flight schedule" style denotion, e.g. 3:45 PM +1d
    isInFuture() && !forceShowDate && useCompactFutureDate -> {
        val formattedTime = DateFormat.getTimeInstance(DateFormat.SHORT, getDeviceLocale()).format(this)
        val daysAhead = TimeUnit.MILLISECONDS.toDays(time - System.currentTimeMillis()).takeIf { it > 0 }
        val formattedDaysAhead = daysAhead?.toSuperscriptString()?.let { "⁺$it" } ?: ""
        "$formattedTime$formattedDaysAhead"
    }

    // This year? Only do not show yar in date
    isThisYear() -> DateUtils.formatDateTime(BaseInjector.get().context(), time, dateFlags or DateUtils.FORMAT_NO_YEAR)

    // Some other time? Format full
    else -> DateUtils.formatDateTime(BaseInjector.get().context(), time, dateFlags)
}

fun Date.isInFuture(): Boolean = this > Date()

fun Date.isToday(): Boolean {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    return format.format(Date()) == format.format(this)
}

fun Date.isThisYear(): Boolean {
    val format = SimpleDateFormat("yyyy", Locale.ENGLISH)
    return format.format(Date()) == format.format(this)
}

fun Number.toSuperscriptString() = toString().map {
    when (it) {
        ',' -> '⋅'
        '.' -> '⋅'
        '-' -> '⁻'
        '+' -> '⁺'
        '0' -> '⁰'
        '1' -> '¹'
        '2' -> '²'
        '3' -> '³'
        '4' -> '⁴'
        '5' -> '⁵'
        '6' -> '⁶'
        '7' -> '⁷'
        '8' -> '⁸'
        '9' -> '⁹'
        else -> it
    }
}.joinToString("")