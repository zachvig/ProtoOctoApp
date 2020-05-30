package de.crysxd.octoapp.base.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintRepository
import okhttp3.logging.HttpLoggingInterceptor

@Module
open class OctoPrintModule {

    @BaseScope
    @Provides
    open fun provideOctoPrintRepository(
        sharedPreferences: SharedPreferences,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ) = OctoPrintRepository(sharedPreferences, httpLoggingInterceptor)

}