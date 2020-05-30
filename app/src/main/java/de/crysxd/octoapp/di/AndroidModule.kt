package de.crysxd.octoapp.di

import android.app.Application
import dagger.Module
import dagger.Provides

@Module
open class AndroidModule(private val app: Application) {

    @Provides
    open fun provideApp() = app

    @Provides
    open fun provideContext() = app.applicationContext

}