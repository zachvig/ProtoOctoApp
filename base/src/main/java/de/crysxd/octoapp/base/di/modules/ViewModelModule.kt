package de.crysxd.octoapp.base.di.modules

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.feedback.SendFeedbackViewModel
import de.crysxd.octoapp.base.ui.BaseViewModelFactory
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueViewModel
import de.crysxd.octoapp.base.ui.common.terminal.TerminalViewModel
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.temperature.ControlBedTemperatureWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.temperature.ControlToolTemperatureWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel
import de.crysxd.octoapp.base.usecase.*
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): BaseViewModelFactory =
        BaseViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(ControlToolTemperatureWidgetViewModel::class)
    open fun provideControlToolTemperatureViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase: SetToolTargetTemperatureUseCase
    ): ViewModel = ControlToolTemperatureWidgetViewModel(octoPrintProvider, useCase)

    @Provides
    @IntoMap
    @ViewModelKey(ControlBedTemperatureWidgetViewModel::class)
    open fun provideControlBedTemperatureViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase: SetBedTargetTemperatureUseCase
    ): ViewModel = ControlBedTemperatureWidgetViewModel(octoPrintProvider, useCase)

    @Provides
    @IntoMap
    @ViewModelKey(SendGcodeWidgetViewModel::class)
    open fun provideSendGcodeWidgetViewModel(
        getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
        useCase: ExecuteGcodeCommandUseCase
    ): ViewModel = SendGcodeWidgetViewModel(getGcodeShortcutsUseCase, useCase)

    @Provides
    @IntoMap
    @ViewModelKey(EnterValueViewModel::class)
    open fun provideEnterValueViewModel(): ViewModel = EnterValueViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(SendFeedbackViewModel::class)
    open fun provideSendFeedbackViewModel(
        sendUseCase: OpenEmailClientForFeedbackUseCase
    ): ViewModel = SendFeedbackViewModel(sendUseCase)

    @Provides
    @IntoMap
    @ViewModelKey(WebcamWidgetViewModel::class)
    open fun provideWebcamWidgetViewModel(
        octoPrintProvider: OctoPrintProvider,
        getWebcamSettingsUseCase: GetWebcamSettingsUseCase
    ): ViewModel = WebcamWidgetViewModel(octoPrintProvider, getWebcamSettingsUseCase)
}