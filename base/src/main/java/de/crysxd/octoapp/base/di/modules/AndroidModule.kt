package de.crysxd.octoapp.base.di.modules

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides

@Module
open class AndroidModule(private val app: Application) {

    @Provides
    open fun provideApp() = app

    @Provides
    open fun provideContext() = app.applicationContext

    @Provides
    open fun sharedPreferences(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)
}