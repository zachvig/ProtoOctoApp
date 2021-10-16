package de.crysxd.baseui.di

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.baseui.BaseViewModelFactory
import de.crysxd.baseui.common.NetworkStateViewModel
import de.crysxd.baseui.common.configureremote.ConfigureRemoteAccessViewModel
import de.crysxd.baseui.common.enter_value.EnterValueViewModel
import de.crysxd.baseui.common.feedback.SendFeedbackViewModel
import de.crysxd.baseui.common.gcode.GcodePreviewViewModel
import de.crysxd.baseui.common.terminal.TerminalViewModel
import de.crysxd.baseui.menu.MenuBottomSheetViewModel
import de.crysxd.baseui.purchase.PurchaseViewModel
import de.crysxd.baseui.widget.EditWidgetsViewModel
import de.crysxd.baseui.widget.extrude.ExtrudeWidgetViewModel
import de.crysxd.baseui.widget.gcode.SendGcodeWidgetViewModel
import de.crysxd.baseui.widget.quickaccess.QuickAccessWidgetViewModel
import de.crysxd.baseui.widget.temperature.ControlTemperatureWidgetViewModel
import de.crysxd.baseui.widget.webcam.WebcamViewModel
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.data.repository.GcodeFileRepository
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.repository.PinnedMenuItemRepository
import de.crysxd.octoapp.base.data.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.data.repository.TemperatureDataRepository
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.ExtrudeFilamentUseCase
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.usecase.GetConnectOctoEverywhereUrlUseCase
import de.crysxd.octoapp.base.usecase.GetGcodeShortcutsUseCase
import de.crysxd.octoapp.base.usecase.GetTerminalFiltersUseCase
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.HandleAutomaticLightEventUseCase
import de.crysxd.octoapp.base.usecase.OpenEmailClientForFeedbackUseCase
import de.crysxd.octoapp.base.usecase.SetAlternativeWebUrlUseCase
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): BaseViewModelFactory =
        BaseViewModelFactory(creators)

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
        getGcodeShortcutsUseCase: GetGcodeShortcutsUseCase,
        octoPrintProvider: OctoPrintProvider,
        octoPreferences: OctoPreferences
    ): ViewModel = SendGcodeWidgetViewModel(
        getGcodeShortcutsUseCase = getGcodeShortcutsUseCase,
        sendGcodeCommandUseCase = useCase,
        octoPreferences = octoPreferences,
        octoPrintProvider = octoPrintProvider,
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
        handleAutomaticLightEventUseCase: HandleAutomaticLightEventUseCase,
    ): ViewModel = WebcamViewModel(
        octoPrintRepository = octoPrintRepository,
        octoPreferences = octoPreferences,
        getWebcamSettingsUseCase = getWebcamSettingsUseCase,
        handleAutomaticLightEventUseCase = handleAutomaticLightEventUseCase
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
        octoPreferences: OctoPreferences,
    ): ViewModel = TerminalViewModel(
        getGcodeShortcutsUseCase,
        executeGcodeCommandUseCase,
        serialCommunicationLogsRepository,
        getTerminalFiltersUseCase,
        octoPrintProvider,
        octoPrintRepository,
        octoPreferences,
    )

    @Provides
    @IntoMap
    @ViewModelKey(GcodePreviewViewModel::class)
    open fun provideGcodePreviewViewModel(
        octoPrintProvider: OctoPrintProvider,
        octoPrintRepository: OctoPrintRepository,
        generateRenderStyleUseCase: GenerateRenderStyleUseCase,
        gcodeFileRepository: GcodeFileRepository,
        octoPreferences: OctoPreferences,
    ): ViewModel = GcodePreviewViewModel(
        octoPrintRepository = octoPrintRepository,
        octoPrintProvider = octoPrintProvider,
        generateRenderStyleUseCase = generateRenderStyleUseCase,
        gcodeFileRepository = gcodeFileRepository,
        octoPreferences = octoPreferences,
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

    @Provides
    @IntoMap
    @ViewModelKey(QuickAccessWidgetViewModel::class)
    fun provideQuickAccessWidgetViewModel(
        pinnedMenuItemRepository: PinnedMenuItemRepository
    ): ViewModel = QuickAccessWidgetViewModel(
        pinnedMenuItemRepository = pinnedMenuItemRepository
    )
}