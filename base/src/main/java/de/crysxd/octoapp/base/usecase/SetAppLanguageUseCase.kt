package de.crysxd.octoapp.base.usecase

import android.app.Activity
import androidx.core.os.bundleOf
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPreferences
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class SetAppLanguageUseCase @Inject constructor(
    private val octoPreferences: OctoPreferences
) : UseCase<SetAppLanguageUseCase.Param, Unit>() {

    override suspend fun doExecute(param: Param, timber: Timber.Tree) {
        timber.i("Setting language to ${param.locale?.language}")
        octoPreferences.appLanguage = param.locale?.language

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