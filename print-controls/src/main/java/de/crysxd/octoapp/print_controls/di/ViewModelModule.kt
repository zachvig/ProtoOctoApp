package de.crysxd.octoapp.print_controls.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.repository.SerialCommunicationLogsRepository
import de.crysxd.octoapp.base.usecase.ExecuteGcodeCommandUseCase
import de.crysxd.octoapp.base.usecase.TogglePausePrintJobUseCase
import de.crysxd.octoapp.base.usecase.TunePrintUseCase
import de.crysxd.octoapp.print_controls.ui.PrintControlsViewModel
import de.crysxd.octoapp.print_controls.ui.widget.TuneFragmentViewModel
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.print_controls.ui.widget.tune.TuneWidgetViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(PrintControlsViewModel::class)
    open fun provideSignInViewModel(
        octoPrintRepository: OctoPrintRepository,
        octoPrintProvider: OctoPrintProvider,
        togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
    ): ViewModel = PrintControlsViewModel(
        octoPrintRepository,
        octoPrintProvider,
        togglePausePrintJobUseCase,
    )

    @Provides
    @IntoMap
    @ViewModelKey(ProgressWidgetViewModel::class)
    open fun provideProgressWidgetViewModel(
        octoPrintProvider: OctoPrintProvider
    ): ViewModel = ProgressWidgetViewModel(octoPrintProvider)

    @Provides
    @IntoMap
    @ViewModelKey(TuneWidgetViewModel::class)
    open fun provideTuneWidgetViewModel(
        serialCommunicationLogsRepository: SerialCommunicationLogsRepository,
        executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
    ): ViewModel = TuneWidgetViewModel(
        serialCommunicationLogsRepository,
        executeGcodeCommandUseCase
    )

    @Provides
    @IntoMap
    @ViewModelKey(TuneFragmentViewModel::class)
    open fun provideTuneFragmentViewModel(
        tunePrintUseCase: TunePrintUseCase
    ): ViewModel = TuneFragmentViewModel(
        tunePrintUseCase
    )
}