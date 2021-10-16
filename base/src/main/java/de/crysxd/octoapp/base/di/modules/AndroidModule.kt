package de.crysxd.octoapp.base.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.usecase.GetAppLanguageUseCase
import kotlinx.coroutines.runBlocking
import javax.inject.Named

@Module
open class AndroidModule(private val app: Application) {

    companion object {
        const val LOCALIZED = "localized"
    }

    @Provides
    open fun provideApp() = app

    @Provides
    open fun provideContext() = app.applicationContext

    @Provides
    @Named(LOCALIZED)
    open fun provideLocalizedContext(appLanguageUseCase: GetAppLanguageUseCase, context: Context): Context {
        val language = runBlocking {
            appLanguageUseCase.execute(Unit)
        }.appLanguageLocale

        return language?.let {
            val config = context.resources.configuration
            config.setLocale(it)
            context.createConfigurationContext(config)
        } ?: context
    }

    @Provides
    open fun sharedPreferences(context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @BaseScope
    open fun provideOctoPreferences(
        sharedPreferences: SharedPreferences,
    ) = OctoPreferences(
        sharedPreferences = sharedPreferences,
        gson = Gson()
    )
}