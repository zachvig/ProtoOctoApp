package de.crysxd.octoapp.print_controls.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.EmergencyStopUseCase
import de.crysxd.octoapp.base.usecase.TogglePausePrintJobUseCase
import de.crysxd.octoapp.print_controls.ui.PrintControlsViewModel
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
        octoPrintProvider: OctoPrintProvider,
        cancelPrintJobUseCase: CancelPrintJobUseCase,
        togglePausePrintJobUseCase: TogglePausePrintJobUseCase,
        emergencyStopUseCase: EmergencyStopUseCase,
        changeFilamentUseCase: ChangeFilamentUseCase
    ): ViewModel = PrintControlsViewModel(octoPrintProvider, cancelPrintJobUseCase, togglePausePrintJobUseCase, emergencyStopUseCase, changeFilamentUseCase)
}