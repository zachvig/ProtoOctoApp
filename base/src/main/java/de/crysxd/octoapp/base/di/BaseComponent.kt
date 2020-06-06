package de.crysxd.octoapp.base.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.modules.*
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.BaseViewModelFactory
import de.crysxd.octoapp.base.usecase.*
import okhttp3.logging.HttpLoggingInterceptor

@BaseScope
@Component(
    modules = [
        AndroidModule::class,
        LoggingModule::class,
        OctoPrintModule::class,
        UseCaseModule::class,
        DataSourceModule::class,
        ViewModelModule::class
    ]
)
interface BaseComponent {

    // AndroidModule
    fun context(): Context
    fun app(): Application

    // LoggingModule
    fun httpLoggingInterceptor(): HttpLoggingInterceptor

    // OctoprintModule
    fun octorPrintRepository(): OctoPrintRepository
    fun octoPrintProvider(): OctoPrintProvider

    // UseCaseModule
    fun setToolTargetTemperatureUseCase(): SetToolTargetTemperatureUseCase
    fun homePrintHeadUseCase(): HomePrintHeadUseCase
    fun jogPrintHeadUseCase(): JogPrintHeadUseCase
    fun turnOnPsuUseCase(): TurnOnPsuUseCase
    fun turnOffPsuUseCase(): TurnOffPsuUseCase
    fun executeGcodeCommandUseCase(): ExecuteGcodeCommandUseCase
    fun extrudeFilamentUseCase(): ExtrudeFilamentUseCase
    fun loadFilesUseCase(): LoadFilesUseCase

    // ViewModelModule
    fun viewModelFactory(): BaseViewModelFactory

}