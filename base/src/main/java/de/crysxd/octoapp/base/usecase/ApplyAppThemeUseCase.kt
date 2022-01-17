package de.crysxd.octoapp.base.usecase

import androidx.appcompat.app.AppCompatDelegate
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.AppTheme
import timber.log.Timber
import javax.inject.Inject

class ApplyAppThemeUseCase @Inject constructor(private val octoPreferences: OctoPreferences) : UseCase<Unit, Unit>() {
    override suspend fun doExecute(param: Unit, timber: Timber.Tree) {
        AppCompatDelegate.setDefaultNightMode(
            when (octoPreferences.appTheme) {
                AppTheme.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}