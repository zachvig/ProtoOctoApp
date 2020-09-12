package de.crysxd.octoapp.base.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.SslKeyStoreHandler

@Module
class SslModule {

    @Provides
    fun provideSslKeyStoreHandler(context: Context) = SslKeyStoreHandler(context)

}