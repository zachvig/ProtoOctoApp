package de.crysxd.octoapp.base.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.SslKeyStoreHandler
import de.crysxd.octoapp.base.di.modules.*
import de.crysxd.octoapp.base.logging.FirebaseTree
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.logging.TimberCacheTree
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.ui.BaseViewModelFactory
import de.crysxd.octoapp.base.usecase.*

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
    fun context(): Context
    fun app(): Application
    fun sharedPreferences(): SharedPreferences

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
    fun gcodeHistoryRepository(): GcodeHistoryRepository

    // UseCaseModule
    fun setToolTargetTemperatureUseCase(): SetToolTargetTemperatureUseCase
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
    fun signOutUseCase(): SignOutUseCase
    fun getAppLanguageUseCase(): GetAppLanguageUseCase
    fun setAppLanguageUseCase(): SetAppLanguageUseCase

    // ViewModelModule
    fun viewModelFactory(): BaseViewModelFactory

}