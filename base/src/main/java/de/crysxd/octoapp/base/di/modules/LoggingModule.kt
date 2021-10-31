package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.logging.TimberHandler

@Module
open class LoggingModule {

    companion object {
        // We had issues with the logs being removed on test failure. Thus we
        // use a "singleton" approach for logging
        private val sensitiveDataMask = SensitiveDataMask()
        private val timberTree = TimberCacheTree(sensitiveDataMask)
    }

    @Provides
    @BaseScope
    open fun provideSensitiveDataMask() = sensitiveDataMask

    @Provides
    @BaseScope
    open fun provideTimberCacheTree(): TimberCacheTree = timberTree

    @Provides
    @BaseScope
    open fun provideFirebaseTree(
        mask: SensitiveDataMask
    ): FirebaseTree = FirebaseTree(
        mask
    )

    @Provides
    @BaseScope
    open fun provideTimberHandler(): TimberHandler = TimberHandler()

}