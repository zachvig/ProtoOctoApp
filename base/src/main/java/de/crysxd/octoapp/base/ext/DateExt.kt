package de.crysxd.octoapp.base.ext

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

// We can't use Locale.getDefault() because we overwrite the app language but we _always_ want to use the user's local preferences for time
private fun getDeviceLocale() = try {
    val deviceLanguage = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
    Locale.forLanguageTag(deviceLanguage)
} catch (e: Exception) {
    // Just precautionary try/catch
    Timber.e(e)
    Locale.getDefault()
}

fun Date.format(forceShowDate: Boolean = false): String = if (isToday() && !forceShowDate) {
    DateFormat.getTimeInstance(DateFormat.SHORT, getDeviceLocale()).format(this)
} else {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getDeviceLocale()).format(this)
}

fun Date.isToday(): Boolean {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    return format.format(Date()) == format.format(this)
}