package de.crysxd.octoapp.base.usecase

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import de.crysxd.octoapp.base.OctoPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ApplyLegacyDarkMode @Inject constructor(private val octoPreferences: OctoPreferences) : UseCase<Unit, Unit>() {
    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        when {
            Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> Timber.i("Manual dark mode not supported, skipping")
            octoPreferences.isManualDarkModeEnabled -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}