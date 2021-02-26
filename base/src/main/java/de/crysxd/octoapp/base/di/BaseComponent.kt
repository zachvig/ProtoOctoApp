package de.crysxd.octoapp.base.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Component
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.SslKeyStoreHandler
import de.crysxd.octoapp.base.di.modules.*
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.repository.*
import de.crysxd.octoapp.base.ui.BaseViewModelFactory
import de.crysxd.octoapp.base.usecase.*
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
        SslModule::class
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

    // OctoprintModule
    fun octorPrintRepository(): OctoPrintRepository
    fun octoPrintProvider(): OctoPrintProvider
    fun serialCommunicationLogsRepository(): SerialCommunicationLogsRepository
    fun gcodeFileRepository(): GcodeFileRepository
    fun pinnedMenuItemsRepository(): PinnedMenuItemRepository
    fun gcodeHistoryRepository(): GcodeHistoryRepository

    // UseCaseModule
    fun setToolTargetTemperatureUseCase(): SetToolTargetTemperatureUseCase
    fun setBedTargetTemperatureUseCase(): SetBedTargetTemperatureUseCase
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

    // ViewModelModule
    fun viewModelFactory(): BaseViewModelFactory

}