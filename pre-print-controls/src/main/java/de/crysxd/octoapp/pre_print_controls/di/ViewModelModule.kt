package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.pre_print_controls.ui.PrePrintControlsViewModel
import de.crysxd.octoapp.pre_print_controls.ui.extrude.ExtrudeControlsViewModel
import de.crysxd.octoapp.pre_print_controls.ui.move.MoveToolControlsViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(PrePrintControlsViewModel::class)
    open fun provideSignInViewModel(
        octoPrintProvider: OctoPrintProvider,
        turnOffPsuUseCase: TurnOffPsuUseCase,
        executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
    ): ViewModel = PrePrintControlsViewModel(octoPrintProvider, turnOffPsuUseCase, executeGcodeCommandUseCase)

    @Provides
    @IntoMap
    @ViewModelKey(MoveToolControlsViewModel::class)
    open fun provideMoveToolControlsViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase1: HomePrintHeadUseCase,
        useCase2: JogPrintHeadUseCase
    ): ViewModel = MoveToolControlsViewModel(useCase1, useCase2, octoPrintProvider)

    @Provides
    @IntoMap
    @ViewModelKey(ExtrudeControlsViewModel::class)
    open fun provideExtrudeControlsViewModel(
        octoPrintProvider: OctoPrintProvider,
        extrudeFilamentUseCase: ExtrudeFilamentUseCase
    ): ViewModel = ExtrudeControlsViewModel(octoPrintProvider, extrudeFilamentUseCase)

}