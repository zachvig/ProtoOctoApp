package de.crysxd.octoapp.base.ext

import android.content.res.Resources
import android.text.format.DateUtils
import androidx.core.os.ConfigurationCompat
import de.crysxd.octoapp.base.di.Injector
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val dateFlags =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_NO_YEAR

// We can't use Locale.getDefault() because we overwrite the app language but we _always_ want to use the user's local preferences for time
private fun getDeviceLocale() = try {
    val deviceLanguage = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
    Locale.forLanguageTag(deviceLanguage)
} catch (e: Exception) {
    // Just precautionary try/catch
    Timber.e(e)
    Locale.getDefault()
}

fun Date.format(forceShowDate: Boolean = false): String = when {
    isToday() && !forceShowDate -> DateFormat.getTimeInstance(DateFormat.SHORT, getDeviceLocale()).format(this)
    isThisYear() -> DateUtils.formatDateTime(Injector.get().context(), time, dateFlags or DateUtils.FORMAT_NO_YEAR)
    else -> DateUtils.formatDateTime(Injector.get().context(), time, dateFlags)
}

fun Date.isToday(): Boolean {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    return format.format(Date()) == format.format(this)
}

fun Date.isThisYear(): Boolean {
    val format = SimpleDateFormat("yyyy", Locale.ENGLISH)
    return format.format(Date()) == format.format(this)
}