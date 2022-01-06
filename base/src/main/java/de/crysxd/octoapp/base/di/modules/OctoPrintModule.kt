package de.crysxd.octoapp.base.di.modules

import android.content.Context
import android.os.Build
import com.github.druk.dnssd.DNSSD
import com.github.druk.dnssd.DNSSDBindable
import com.github.druk.dnssd.DNSSDEmbedded
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.GcodeFileRepository
import de.crysxd.octoapp.base.data.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.data.repository.MediaFileRepository
import de.crysxd.octoapp.base.data.repository.NotificationIdRepository
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.repository.PinnedMenuItemRepository
import de.crysxd.octoapp.base.data.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.data.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.data.repository.TimelapseRepository
import de.crysxd.octoapp.base.data.repository.TutorialsRepository
import de.crysxd.octoapp.base.data.repository.WidgetPreferencesRepository
import de.crysxd.octoapp.base.data.source.DataSource
import de.crysxd.octoapp.base.data.source.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.data.source.LocalMediaFileDataSource
import de.crysxd.octoapp.base.data.source.LocalPinnedMenuItemsDataSource
import de.crysxd.octoapp.base.data.source.RemoteGcodeFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteMediaFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteTutorialsDataSource
import de.crysxd.octoapp.base.data.source.WidgetPreferencesDataSource
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.network.CachedLocalDnsResolver
import de.crysxd.octoapp.base.network.DefaultLocalDnsResolver
import de.crysxd.octoapp.base.network.DetectBrokenSetupInterceptor
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.network.SslKeyStoreHandler
import javax.inject.Named

@Module
open class OctoPrintModule {

    @BaseScope
    @Provides
    open fun provideOctoPrintRepository(
        dataSource: DataSource<List<OctoPrintInstanceInformationV3>>,
        octoPreferences: OctoPreferences,
        sensitiveDataMask: SensitiveDataMask
    ) = OctoPrintRepository(
        dataSource = dataSource,
        octoPreferences = octoPreferences,
        sensitiveDataMask = sensitiveDataMask,
    )

    @BaseScope
    @Provides
    open fun provideOctoPrintProvider(
        detectBrokenSetupInterceptor: DetectBrokenSetupInterceptor,
        octoPrintRepository: OctoPrintRepository,
        analytics: FirebaseAnalytics,
        sslKeyStoreHandler: SslKeyStoreHandler,
        localDnsResolver: LocalDnsResolver
    ) = OctoPrintProvider(detectBrokenSetupInterceptor, octoPrintRepository, analytics, sslKeyStoreHandler, localDnsResolver)

    @BaseScope
    @Provides
    open fun provideInvalidApiKeyInterceptor(
        @Named(AndroidModule.LOCALIZED) context: Context,
        octoPrintRepository: OctoPrintRepository,
    ) = DetectBrokenSetupInterceptor(
        context = context,
        octoPrintRepository = octoPrintRepository
    )

    @BaseScope
    @Provides
    open fun provideGcodeHistoryRepository(
        dataSource: DataSource<List<GcodeHistoryItem>>
    ) = GcodeHistoryRepository(dataSource)

    @BaseScope
    @Provides
    open fun providePinnedMenuItemRepository(
        dataSource: LocalPinnedMenuItemsDataSource,
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
    open fun provideNotificationIdRepository(
        octoPrintRepository: OctoPrintRepository,
        context: Context,
    ) = NotificationIdRepository(
        octoPrintRepository = octoPrintRepository,
        sharedPreferences = context.getSharedPreferences("notification_id_cache", Context.MODE_PRIVATE)
    )

    @BaseScope
    @Provides
    open fun provideTutorialsRepository(
        tutorialsDataSource: RemoteTutorialsDataSource,
    ) = TutorialsRepository(
        dataSource = tutorialsDataSource,
    )

    @BaseScope
    @Provides
    open fun provideLocalDnsResolver(
        context: Context,
        dnssd: DNSSD,
    ): LocalDnsResolver = if (Firebase.remoteConfig.getBoolean("use_cached_dns_resolver")) {
        CachedLocalDnsResolver(context, dnssd)
    } else {
        DefaultLocalDnsResolver(context, dnssd)
    }

    @BaseScope
    @Provides
    open fun provideDNSSD(
        context: Context,
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        DNSSDEmbedded(context)
    } else {
        DNSSDBindable(context)
    }

    @BaseScope
    @Provides
    open fun provideMediaFileRepository(
        localMediaFileDataSource: LocalMediaFileDataSource,
        remoteMediaFileDataSource: RemoteMediaFileDataSource,
    ) = MediaFileRepository(
        localDataSource = localMediaFileDataSource,
        remoteDataSource = remoteMediaFileDataSource
    )

    @BaseScope
    @Provides
    open fun provideTimelapseRepository(
        octoPrintProvider: OctoPrintProvider,
        localMediaFileDataSource: LocalMediaFileDataSource,
        remoteMediaFileDataSource: RemoteMediaFileDataSource,
    ) = TimelapseRepository(
        octoPrintProvider = octoPrintProvider,
        localMediaFileDataSource = localMediaFileDataSource,
        remoteMediaFileDataSource = remoteMediaFileDataSource,
    )
}