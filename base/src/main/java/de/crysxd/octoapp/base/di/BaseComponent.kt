package de.crysxd.octoapp.base.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Component
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.repository.GcodeFileRepository
import de.crysxd.octoapp.base.data.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.data.repository.NotificationIdRepository
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.repository.PinnedMenuItemRepository
import de.crysxd.octoapp.base.data.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.data.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.data.repository.TutorialsRepository
import de.crysxd.octoapp.base.data.repository.WidgetPreferencesRepository
import de.crysxd.octoapp.base.data.source.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.di.modules.AndroidModule
import de.crysxd.octoapp.base.di.modules.DataSourceModule
import de.crysxd.octoapp.base.di.modules.FirebaseModule
import de.crysxd.octoapp.base.di.modules.LoggingModule
import de.crysxd.octoapp.base.di.modules.OctoPrintModule
import de.crysxd.octoapp.base.di.modules.SslModule
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.network.SslKeyStoreHandler
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import de.crysxd.octoapp.base.usecase.ApplyLegacyDarkMode
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.CreateFolderUseCase
import de.crysxd.octoapp.base.usecase.CreateProgressAppWidgetDataUseCase
import de.crysxd.octoapp.base.usecase.CyclePsuUseCase
import de.crysxd.octoapp.base.usecase.DeleteFileUseCase
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.base.usecase.EmergencyStopUseCase
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.ExecuteSystemCommandUseCase
import de.crysxd.octoapp.base.usecase.ExtrudeFilamentUseCase
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.base.usecase.GetAppLanguageUseCase
import de.crysxd.octoapp.base.usecase.GetMaterialsUseCase
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.GetWebcamSnapshotUseCase
import de.crysxd.octoapp.base.usecase.HandleOctoEverywhereAppPortalSuccessUseCase
import de.crysxd.octoapp.base.usecase.HandleOctoEverywhereExceptionUseCase
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.MoveFileUseCase
import de.crysxd.octoapp.base.usecase.OpenEmailClientForFeedbackUseCase
import de.crysxd.octoapp.base.usecase.OpenOctoprintWebUseCase
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.base.usecase.TakeScreenshotUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.base.usecase.TogglePausePrintJobUseCase
import de.crysxd.octoapp.base.usecase.TogglePsuUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.base.usecase.TurnOnPsuUseCase
import de.crysxd.octoapp.base.usecase.UpdateInstanceCapabilitiesUseCase
import javax.inject.Named

@BaseScope
@Component(
    modules = [
        AndroidModule::class,
        LoggingModule::class,
        OctoPrintModule::class,
        DataSourceModule::class,
        FirebaseModule::class,
        SslModule::class,
    ]
)
interface BaseComponent {

    // AndroidModule
    @Named(AndroidModule.LOCALIZED)
    fun localizedContext(): Context
    fun context(): Context
    fun app(): Application
    fun sharedPreferences(): SharedPreferences
    fun octoPreferences(): OctoPreferences

    // SslModule
    fun sslKeyStoreHandler(): SslKeyStoreHandler

    // LoggingModule
    fun timberCacheTree(): TimberCacheTree
    fun firebaseTree(): FirebaseTree
    fun sensitiveDataMask(): SensitiveDataMask

    // DataSourceModule
    fun localGcodeFileDataSource(): LocalGcodeFileDataSource

    // OctoprintModule
    fun octorPrintRepository(): OctoPrintRepository
    fun octoPrintProvider(): OctoPrintProvider
    fun serialCommunicationLogsRepository(): SerialCommunicationLogsRepository
    fun gcodeFileRepository(): GcodeFileRepository
    fun pinnedMenuItemsRepository(): PinnedMenuItemRepository
    fun gcodeHistoryRepository(): GcodeHistoryRepository
    fun widgetPreferencesRepository(): WidgetPreferencesRepository
    fun localDnsResolver(): LocalDnsResolver
    fun notificationIdRepository(): NotificationIdRepository
    fun temperatureDataRepository(): TemperatureDataRepository
    fun tutorialsRepository(): TutorialsRepository

    // UseCaseModule
    fun setTargetTemperatureUseCase(): SetTargetTemperaturesUseCase
    fun homePrintHeadUseCase(): HomePrintHeadUseCase
    fun jogPrintHeadUseCase(): JogPrintHeadUseCase
    fun turnOnPsuUseCase(): TurnOnPsuUseCase
    fun turnOffPsuUseCase(): TurnOffPsuUseCase
    fun executeGcodeCommandUseCase(): ExecuteGcodeCommandUseCase
    fun extrudeFilamentUseCase(): ExtrudeFilamentUseCase
    fun loadFilesUseCase(): LoadFilesUseCase
    fun startPrintJobUseCase(): StartPrintJobUseCase
    fun cancelPrintJobUseCase(): CancelPrintJobUseCase
    fun togglePausePrintJobUseCase(): TogglePausePrintJobUseCase
    fun emergencyStopUseCase(): EmergencyStopUseCase
    fun openOctoPrintWebUseCase(): OpenOctoprintWebUseCase
    fun openEmailClientForFeedbackUseCase(): OpenEmailClientForFeedbackUseCase
    fun takeScreenshotUseCase(): TakeScreenshotUseCase
    fun formatDurationUseCase(): FormatDurationUseCase
    fun updateInstanceCapabilitiesUseCase(): UpdateInstanceCapabilitiesUseCase
    fun formatEtaUseCase(): FormatEtaUseCase
    fun getAppLanguageUseCase(): GetAppLanguageUseCase
    fun setAppLanguageUseCase(): SetAppLanguageUseCase
    fun getPowerDevicesUseCase(): GetPowerDevicesUseCase
    fun applyLegacyDarkModeUseCase(): ApplyLegacyDarkMode
    fun executeSystemCommandUseCase(): ExecuteSystemCommandUseCase
    fun getWebcamSettingsUseCase(): GetWebcamSettingsUseCase
    fun getWebcamSnapshotUseCase(): GetWebcamSnapshotUseCase
    fun createProgressAppWidgetDataUseCase(): CreateProgressAppWidgetDataUseCase
    fun getMaterialsUseCase(): GetMaterialsUseCase
    fun activateMaterialUseCase(): ActivateMaterialUseCase
    fun cyclePsuUseCase(): CyclePsuUseCase
    fun togglePsuUseCase(): TogglePsuUseCase
    fun handleOctoEverywhereAppPortalSuccessUseCase(): HandleOctoEverywhereAppPortalSuccessUseCase
    fun handleOctoEverywhereExceptionUseCase(): HandleOctoEverywhereExceptionUseCase
    fun discoverOctoPrintUseCase(): DiscoverOctoPrintUseCase
    fun requestApiAccessUseCase(): RequestApiAccessUseCase
    fun testFullNetworkStackUseCase(): TestFullNetworkStackUseCase
    fun deleteFileUseCase(): DeleteFileUseCase
    fun moveFileUseCase(): MoveFileUseCase
    fun createFolderUseCase(): CreateFolderUseCase

}