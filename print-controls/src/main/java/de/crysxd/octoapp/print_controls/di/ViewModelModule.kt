package de.crysxd.octoapp.print_controls.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.EmergencyStopUseCase
import de.crysxd.octoapp.base.usecase.TogglePausePrintJobUseCase
import de.crysxd.octoapp.print_controls.ui.PrintControlsViewModel
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
        cancelPrintJobUseCase: CancelPrintJobUseCase,
        togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
        emergencyStopUseCase: EmergencyStopUseCase,
        changeFilamentUseCase: ChangeFilamentUseCase
    ): ViewModel = PrintControlsViewModel(
        octoPrintRepository,
        octoPrintProvider,
        cancelPrintJobUseCase,
        togglePausePrintJobUseCase,
        emergencyStopUseCase,
        changeFilamentUseCase
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
    ): ViewModel = TuneWidgetViewModel()
}