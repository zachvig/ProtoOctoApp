package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.TimberCacheTree
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

@Module
open class LoggingModule {

    @Provides
    open fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            val tag = "HTTP"
            override fun log(message: String) {
                if (message.startsWith("-->") || message.startsWith("<--")) {
                    Timber.tag(tag).i(message)
                } else {
                    Timber.tag(tag).v(message)
                }
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)

    @Provides
    @BaseScope
    open fun provideTimberCacheTree(): TimberCacheTree = TimberCacheTree()

    @Provides
    @BaseScope
    open fun provideFirebaseTree(): FirebaseTree = FirebaseTree()

}