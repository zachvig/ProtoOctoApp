package de.crysxd.octoapp.base.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.di.BaseScope

@Module
open class AndroidModule(private val app: Application) {

    @Provides
    open fun provideApp() = app

    @Provides
    open fun provideContext() = app.applicationContext

    @Provides
    open fun sharedPreferences(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @BaseScope
    open fun provideOctoPreferences(sharedPreferences: SharedPreferences) =
        OctoPreferences(sharedPreferences)
}