package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.logging.TimberHandler

@Module
open class LoggingModule {

    @Provides
    @BaseScope
    open fun provideTimberCacheTree(): TimberCacheTree = TimberCacheTree()

    @Provides
    @BaseScope
    open fun provideFirebaseTree(): FirebaseTree = FirebaseTree()

    @Provides
    @BaseScope
    open fun provideTimberHandler(): TimberHandler = TimberHandler()

}