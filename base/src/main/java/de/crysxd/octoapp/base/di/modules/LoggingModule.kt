package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

@Module
open class LoggingModule {

    @Provides
    open fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Timber.tag("HTTP").i(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)


}