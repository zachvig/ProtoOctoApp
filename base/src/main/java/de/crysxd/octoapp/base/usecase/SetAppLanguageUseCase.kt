package de.crysxd.octoapp.base.usecase

import android.app.Activity
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.os.bundleOf
import de.crysxd.octoapp.base.OctoAnalytics
import timber.log.Timber
import java.util.*
import javax.inject.Inject

const val KEY_APP_LANGUAGE = "app_language"

class SetAppLanguageUseCase @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : UseCase<SetAppLanguageUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        sharedPreferences.edit {
            timber.i("Setting language to ${param.locale?.language}")
            putString(KEY_APP_LANGUAGE, param.locale?.language)
        }

        OctoAnalytics.setUserProperty(OctoAnalytics.UserProperty.AppLanguage, param.locale?.language)
        OctoAnalytics.logEvent(
            OctoAnalytics.Event.AppLanguageChanged,
            bundleOf(
                "language" to (param.locale?.language ?: "reset")
            )
        )

        param.activity.finish()
        param.activity.startActivity(param.activity.intent)
    }

    data class Param(
        val locale: Locale?,
        val activity: Activity
    )
}