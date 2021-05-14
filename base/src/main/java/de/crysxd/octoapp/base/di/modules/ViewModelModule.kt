package de.crysxd.octoapp.base.di.modules

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.billing.PurchaseViewModel
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.feedback.SendFeedbackViewModel
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModelFactory
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.configureremote.ConfigureRemoteAccessViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueViewModel
import de.crysxd.octoapp.base.ui.common.gcode.GcodePreviewViewModel
import de.crysxd.octoapp.base.ui.common.terminal.TerminalViewModel
import de.crysxd.octoapp.base.ui.common.troubleshoot.TroubleShootViewModel
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetViewModel
import de.crysxd.octoapp.base.ui.widget.EditWidgetsViewModel
import de.crysxd.octoapp.base.ui.widget.extrude.ExtrudeWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel
import de.crysxd.octoapp.base.usecase.*
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): BaseViewModelFactory =
        BaseViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(TroubleShootViewModel::class)
    open fun provideTroubleShootViewModel(): ViewModel =
        TroubleShootViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ControlTemperatureWidgetViewModel::class)
    open fun provideControlToolTemperatureViewModel(
        temperatureDataRepository: TemperatureDataRepository,
        octoPrintRepository: OctoPrintRepository,
        setTargetTemperaturesUseCase: SetTargetTemperaturesUseCase,
    ): ViewModel = ControlTemperatureWidgetViewModel(
        temperatureDataRepository = temperatureDataRepository,
        setTargetTemperaturesUseCase = setTargetTemperaturesUseCase,
        octoPrintRepository = octoPrintRepository,
    )

    @Provides
    @IntoMap
    @ViewModelKey(SendGcodeWidgetViewModel::class)
    open fun provideSendGcodeWidgetViewModel(
        useCase: ExecuteGcodeCommandUseCase,
        getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase
    ): ViewModel = SendGcodeWidgetViewModel(
        getGcodeShortcutsUseCase = getGcodeShortcutsUseCase,
        sendGcodeCommandUseCase = useCase
    )

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
    @ViewModelKey(WebcamViewModel::class)
    open fun provideWebcamWidgetViewModel(
        octoPrintRepository: OctoPrintRepository,
        octoPreferences: OctoPreferences,
        getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
        handleAutomaticIlluminationEventUseCase: HandleAutomaticIlluminationEventUseCase,
    ): ViewModel = WebcamViewModel(
        octoPrintRepository = octoPrintRepository,
        octoPreferences = octoPreferences,
        getWebcamSettingsUseCase = getWebcamSettingsUseCase,
        handleAutomaticIlluminationEventUseCase = handleAutomaticIlluminationEventUseCase
    )

    @Provides
    @IntoMap
    @ViewModelKey(TerminalViewModel::class)
    open fun provideTerminalViewModel(
        getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
        executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
        serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
        getTerminalFiltersUseCase: GetTerminalFiltersUseCase,
        octoPrintProvider: OctoPrintProvider,
        octoPrintRepository: OctoPrintRepository,
    ): ViewModel = TerminalViewModel(
        getGcodeShortcutsUseCase,
        executeGcodeCommandUseCase,
        serialCommunicationLogsRepository,
        getTerminalFiltersUseCase,
        octoPrintProvider,
        octoPrintRepository,
    )

    @Provides
    @IntoMap
    @ViewModelKey(GcodePreviewViewModel::class)
    open fun provideGcodePreviewViewModel(
        octoPrintProvider: OctoPrintProvider,
        octoPrintRepository: OctoPrintRepository,
        generateRenderStyleUseCase: GenerateRenderStyleUseCase,
        getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
        gcodeFileRepository: GcodeFileRepository
    ): ViewModel = GcodePreviewViewModel(
        octoPrintRepository = octoPrintRepository,
        octoPrintProvider = octoPrintProvider,
        generateRenderStyleUseCase = generateRenderStyleUseCase,
        getCurrentPrinterProfileUseCase = getCurrentPrinterProfileUseCase,
        gcodeFileRepository = gcodeFileRepository
    )

    @Provides
    @IntoMap
    @ViewModelKey(PurchaseViewModel::class)
    open fun providePurchaseViewModel(
    ): ViewModel = PurchaseViewModel(
    )

    @Provides
    @IntoMap
    @ViewModelKey(EditWidgetsViewModel::class)
    open fun provideEditWidgetsViewModel(
    ): ViewModel = EditWidgetsViewModel(
    )

    @Provides
    @IntoMap
    @ViewModelKey(NetworkStateViewModel::class)
    open fun provideNetworkStateViewModel(
        application: Application
    ): ViewModel = NetworkStateViewModel(
        application = application
    )

    @Provides
    @IntoMap
    @ViewModelKey(MenuBottomSheetViewModel::class)
    open fun provideMenuBottomSheetViewModel(): ViewModel = MenuBottomSheetViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(ConfigureRemoteAccessViewModel::class)
    open fun provideConfigureRemoteAccessViewModel(
        octoPrintRepository: OctoPrintRepository,
        setAlternativeWebUrlUseCase: SetAlternativeWebUrlUseCase,
        getConnectOctoEverywhereUrlUseCase: GetConnectOctoEverywhereUrlUseCase,
    ): ViewModel = ConfigureRemoteAccessViewModel(
        octoPrintRepository = octoPrintRepository,
        setAlternativeWebUrlUseCase = setAlternativeWebUrlUseCase,
        getConnectOctoEverywhereUrlUseCase = getConnectOctoEverywhereUrlUseCase,
    )

    @Provides
    @IntoMap
    @ViewModelKey(ExtrudeWidgetViewModel::class)
    open fun provideExtrudeWidgetViewModel(
        setToolTargetTemperatureUseCase: SetTargetTemperaturesUseCase,
        extrudeFilamentUseCase: ExtrudeFilamentUseCase,
        octoPrintProvider: OctoPrintProvider,
    ): ViewModel = ExtrudeWidgetViewModel(
        extrudeFilamentUseCase,
        setToolTargetTemperatureUseCase,
        octoPrintProvider
    )
}