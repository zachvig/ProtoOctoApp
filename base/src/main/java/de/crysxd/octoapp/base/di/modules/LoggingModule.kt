package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.TimberCacheTree
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

@Module
open class LoggingModule {

    @Provides
    open fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Timber.tag("HTTP").v(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)

    @Provides
    @BaseScope
    open fun provideTimberCacheTree(): TimberCacheTree = TimberCacheTree()

}