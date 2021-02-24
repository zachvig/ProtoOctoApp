package de.crysxd.octoapp.base.di.modules

import android.app.Application
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.billing.PurchaseViewModel
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.feedback.SendFeedbackViewModel
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.ui.BaseViewModelFactory
import de.crysxd.octoapp.base.ui.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueViewModel
import de.crysxd.octoapp.base.ui.common.gcode.GcodePreviewViewModel
import de.crysxd.octoapp.base.ui.common.gcodeshortcut.GcodeShortcutEditViewModel
import de.crysxd.octoapp.base.ui.common.terminal.TerminalViewModel
import de.crysxd.octoapp.base.ui.common.troubleshoot.TroubleShootViewModel
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetViewModel
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.temperature.ControlBedTemperatureWidgetViewModel
import de.crysxd.octoapp.base.ui.widget.temperature.ControlToolTemperatureWidgetViewModel
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
        getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
        applyWebcamTransformationsUseCase: ApplyWebcamTransformationsUseCase,
    ): ViewModel = WebcamViewModel(
        octoPrintRepository,
        getWebcamSettingsUseCase,
        applyWebcamTransformationsUseCase,
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
    @ViewModelKey(GcodeShortcutEditViewModel::class)
    open fun provideGcodeShortcutEditViewModel(
        gcodeHistoryRepository: GcodeHistoryRepository
    ): ViewModel = GcodeShortcutEditViewModel(
        gcodeHistoryRepository = gcodeHistoryRepository
    )

    @Provides
    @IntoMap
    @ViewModelKey(PurchaseViewModel::class)
    open fun providePurchaseViewModel(
    ): ViewModel = PurchaseViewModel(
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
}