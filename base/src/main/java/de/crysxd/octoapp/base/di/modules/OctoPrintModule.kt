package de.crysxd.octoapp.base.di.modules

import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.InvalidApiKeyInterceptor
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase

@Module
open class OctoPrintModule {

    @BaseScope
    @Provides
    open fun provideOctoPrintRepository(
        dataSource: DataSource<OctoPrintInstanceInformationV2>,
        checkOctoPrintInstanceInformationUseCase: CheckOctoPrintInstanceInformationUseCase
    ) = OctoPrintRepository(
        dataSource,
        checkOctoPrintInstanceInformationUseCase
    )

    @BaseScope
    @Provides
    open fun provideOctoPrintProvider(
        timberHandler: TimberHandler,
        invalidApiKeyInterceptor: InvalidApiKeyInterceptor,
        octoPrintRepository: OctoPrintRepository,
        analytics: FirebaseAnalytics
    ) = OctoPrintProvider(timberHandler, invalidApiKeyInterceptor, octoPrintRepository, analytics)

    @BaseScope
    @Provides
    open fun provideInvalidApiKeyInterceptor(
        octoPrintRepository: OctoPrintRepository
    ) = InvalidApiKeyInterceptor(octoPrintRepository)

}