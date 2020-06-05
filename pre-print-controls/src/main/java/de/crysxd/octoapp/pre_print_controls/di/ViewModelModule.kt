package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import de.crysxd.octoapp.pre_print_controls.ui.PrePrintControlsViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(PrePrintControlsViewModel::class)
    open fun provideSignInViewModel(): ViewModel = PrePrintControlsViewModel()

    @Provides
    @IntoMap
    @ViewModelKey(de.crysxd.octoapp.pre_print_controls.ui.move.MoveToolControlsViewModel::class)
    open fun provideMoveToolControlsViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase1: HomePrintHeadUseCase,
        useCase2: JogPrintHeadUseCase
    ): ViewModel = de.crysxd.octoapp.pre_print_controls.ui.move.MoveToolControlsViewModel(useCase1, useCase2, octoPrintProvider)

}