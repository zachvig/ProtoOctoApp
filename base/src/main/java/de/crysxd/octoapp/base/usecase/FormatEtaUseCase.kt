package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
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

        // We enforece the usage of the device language for time formatting; user might use OctoApp in English
        // but does for sure not want AM/PM
        val locale = try {
            val deviceLanguage = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
            Locale.forLanguageTag(deviceLanguage)
        } catch (e: Exception) {
            // Just precautionary try/catch
            Locale.getDefault()
        }

        return when {
            lessThanAnHour && param.allowRelative -> context.getString(R.string.x_left, formatDurationUseCase.execute(param.secsLeft))
            eta.isToday() -> context.getString(R.string.eta_x, DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(eta))
            else -> context.getString(R.string.eta_x, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(eta))
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