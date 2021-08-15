package de.crysxd.octoapp.base.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Component
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.di.modules.AndroidModule
import de.crysxd.octoapp.base.di.modules.DataSourceModule
import de.crysxd.octoapp.base.di.modules.FirebaseModule
import de.crysxd.octoapp.base.di.modules.LoggingModule
import de.crysxd.octoapp.base.di.modules.OctoPrintModule
import de.crysxd.octoapp.base.di.modules.SslModule
import de.crysxd.octoapp.base.di.modules.ViewModelModule
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.base.network.SslKeyStoreHandler
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.PinnedMenuItemRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.repository.WidgetPreferencesRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModelFactory
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import de.crysxd.octoapp.base.usecase.ApplyLegacyDarkMode
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.CreateProgressAppWidgetDataUseCase
import de.crysxd.octoapp.base.usecase.CyclePsuUseCase
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
import de.crysxd.octoapp.base.usecase.OpenEmailClientForFeedbackUseCase
import de.crysxd.octoapp.base.usecase.OpenOctoprintWebUseCase
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import de.crysxd.octoapp.base.usecase.SetAppLanguageUseCase
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.base.usecase.TakeScreenshotUseCase
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
        ViewModelModule::class,
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

    // ViewModelModule
    fun viewModelFactory(): BaseViewModelFactory

}