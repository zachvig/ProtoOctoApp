package de.crysxd.octoapp.base.usecase

import android.app.Activity
import android.content.SharedPreferences
import androidx.core.content.edit
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

        param.activity.finish()
        param.activity.startActivity(param.activity.intent)
    }

    data class Param(
        val locale: Locale?,
        val activity: Activity
    )
}