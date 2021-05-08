package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.pre_print_controls.ui.PrePrintControlsViewModel
import de.crysxd.octoapp.base.ui.widget.extrude.ExtrudeWidgetViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidgetViewModel
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
        octoPrintRepository: OctoPrintRepository,
        turnOffPsuUseCase: TurnOffPsuUseCase,
        changeFilamentUseCase: ChangeFilamentUseCase
    ): ViewModel = PrePrintControlsViewModel(
        octoPrintRepository,
        turnOffPsuUseCase,
        changeFilamentUseCase
    )

    @Provides
    @IntoMap
    @ViewModelKey(MoveToolWidgetViewModel::class)
    open fun provideMoveToolControlsViewModel(
        octoPrintRepository: OctoPrintRepository,
        useCase1: HomePrintHeadUseCase,
        useCase2: JogPrintHeadUseCase
    ): ViewModel = MoveToolWidgetViewModel(
        useCase1,
        useCase2,
        octoPrintRepository
    )
}