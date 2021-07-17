package de.crysxd.octoapp.base.di.modules

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.datasource.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.datasource.RemoteGcodeFileDataSource
import de.crysxd.octoapp.base.datasource.WidgetPreferencesDataSource
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberHandler
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.network.DetectBrokenSetupInterceptor
import de.crysxd.octoapp.base.network.SslKeyStoreHandler
import de.crysxd.octoapp.base.network.dns.LocalDnsResolver
import de.crysxd.octoapp.base.repository.*

@Module
open class OctoPrintModule {

    @BaseScope
    @Provides
    open fun provideOctoPrintRepository(
        legacyDataSource: DataSource<OctoPrintInstanceInformationV2>,
        dataSource: DataSource<List<OctoPrintInstanceInformationV2>>,
        octoPreferences: OctoPreferences,
        sensitiveDataMask: SensitiveDataMask
    ) = OctoPrintRepository(
        legacyDataSource,
        dataSource,
        octoPreferences,
        sensitiveDataMask,
    )

    @BaseScope
    @Provides
    open fun provideOctoPrintProvider(
        timberHandler: TimberHandler,
        detectBrokenSetupInterceptor: DetectBrokenSetupInterceptor,
        octoPrintRepository: OctoPrintRepository,
        analytics: FirebaseAnalytics,
        sslKeyStoreHandler: SslKeyStoreHandler,
        localDnsResolver: LocalDnsResolver
    ) = OctoPrintProvider(timberHandler, detectBrokenSetupInterceptor, octoPrintRepository, analytics, sslKeyStoreHandler, localDnsResolver)

    @BaseScope
    @Provides
    open fun provideInvalidApiKeyInterceptor(
        octoPrintRepository: OctoPrintRepository
    ) = DetectBrokenSetupInterceptor(octoPrintRepository)

    @BaseScope
    @Provides
    open fun provideGcodeHistoryRepository(
        dataSource: DataSource<List<GcodeHistoryItem>>
    ) = GcodeHistoryRepository(dataSource)

    @BaseScope
    @Provides
    open fun providePinnedMenuItemRepository(
        dataSource: DataSource<Set<String>>
    ) = PinnedMenuItemRepository(dataSource)

    @BaseScope
    @Provides
    open fun provideFileRepository(
        localDataSource: LocalGcodeFileDataSource,
        remoteDataSource: RemoteGcodeFileDataSource,
    ) = GcodeFileRepository(localDataSource, remoteDataSource)

    @BaseScope
    @Provides
    open fun provideSerialCommunicationLogsRepository(
        octoPrintProvider: OctoPrintProvider
    ) = SerialCommunicationLogsRepository(octoPrintProvider)

    @BaseScope
    @Provides
    open fun provideWidgetOrderRepository(
        dataSource: WidgetPreferencesDataSource
    ) = WidgetPreferencesRepository(dataSource)

    @BaseScope
    @Provides
    open fun provideTemperatureDataRepository(
        octoPrintProvider: OctoPrintProvider
    ) = TemperatureDataRepository(octoPrintProvider)

    @BaseScope
    @Provides
    open fun provideLocalDnsResolver(
        context: Context
    ) = LocalDnsResolver(context)
}