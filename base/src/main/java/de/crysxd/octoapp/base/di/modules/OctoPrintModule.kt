package de.crysxd.octoapp.base.di.modules

import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.InvalidApiKeyInterceptor
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.SslKeyStoreHandler
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.datasource.GcodeFileDataSourceGroup
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
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
        analytics: FirebaseAnalytics,
        sslKeyStoreHandler: SslKeyStoreHandler
    ) = OctoPrintProvider(timberHandler, invalidApiKeyInterceptor, octoPrintRepository, analytics, sslKeyStoreHandler)

    @BaseScope
    @Provides
    open fun provideInvalidApiKeyInterceptor(
        octoPrintRepository: OctoPrintRepository
    ) = InvalidApiKeyInterceptor(octoPrintRepository)

    @BaseScope
    @Provides
    open fun provideGcodeHistoryRepository(
        dataSource: DataSource<List<GcodeHistoryItem>>
    ) = GcodeHistoryRepository(dataSource)

    @BaseScope
    @Provides
    open fun provideFileRepository(
        dataSources: GcodeFileDataSourceGroup
    ) = GcodeFileRepository(dataSources)

    @BaseScope
    @Provides
    open fun provideSerialCommunicationLogsRepository(
        octoPrintProvider: OctoPrintProvider
    ) = SerialCommunicationLogsRepository(octoPrintProvider)
}