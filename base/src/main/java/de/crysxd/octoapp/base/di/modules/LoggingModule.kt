package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.repository.OctoPrintRepository

@Module
open class LoggingModule {

    @Provides
    @BaseScope
    open fun provideSensitiveDataMask(
        octoPrintRepository: OctoPrintRepository
    ): SensitiveDataMask = SensitiveDataMask(
        octoPrintRepository
    )

    @Provides
    @BaseScope
    open fun provideTimberCacheTree(
        mask: SensitiveDataMask
    ): TimberCacheTree = TimberCacheTree(
        mask
    )

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