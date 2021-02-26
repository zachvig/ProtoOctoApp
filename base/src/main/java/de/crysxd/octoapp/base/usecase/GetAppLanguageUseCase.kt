package de.crysxd.octoapp.base.usecase

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import de.crysxd.octoapp.base.OctoPreferences
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class GetAppLanguageUseCase @Inject constructor(
    private val octoPreferences: OctoPreferences
) : UseCase<Unit, GetAppLanguageUseCase.Result>() {

    init {
        suppressLogging = true
    }

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): Result {
        val confirmedLanguages = listOf("de", "fr") // If device language is listed here, it will be used as default
        val deviceLanguage = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0].language
        val appLanguage = octoPreferences.appLanguage ?: deviceLanguage.takeIf { confirmedLanguages.contains(it) } ?: "en"

        timber.i("Device language: $deviceLanguage")
        timber.i("App language: $appLanguage")

        val switchLanguageText = when {
            appLanguage != "en" -> "Use OctoApp in English"
            deviceLanguage == "de" -> "Nutze OctoApp in Deutsch"
            deviceLanguage == "nl" -> "Gebruik in het Nederlands (Beta)"
            deviceLanguage == "fr" -> "Utilisation en français"
            deviceLanguage == "es" -> "Uso en español (Beta)"
            deviceLanguage == "it" -> "Uso in italiano (Beta)"
            else -> null
        }

        val switchLanguageLocale = when {
            appLanguage != "en" -> Locale.forLanguageTag("en")
            listOf("de", "nl", "it", "es", "fr").contains(deviceLanguage) -> Locale.forLanguageTag(deviceLanguage)
            else -> null
        }

        return Result(
            appLanguageLocale = appLanguage?.let { Locale.forLanguageTag(it) },
            canSwitchLocale = switchLanguageText != null,
            switchLanguageText = switchLanguageText,
            switchLanguageLocale = switchLanguageLocale
        )
    }

    data class Result(
        val appLanguageLocale: Locale?,
        val canSwitchLocale: Boolean,
        val switchLanguageText: String?,
        val switchLanguageLocale: Locale?
    )
}