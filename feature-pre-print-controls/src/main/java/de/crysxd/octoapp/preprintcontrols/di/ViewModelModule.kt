package de.crysxd.octoapp.preprintcontrols.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.usecase.ChangeFilamentUseCase
import de.crysxd.octoapp.base.usecase.HomePrintHeadUseCase
import de.crysxd.octoapp.base.usecase.JogPrintHeadUseCase
import de.crysxd.octoapp.base.usecase.TurnOffPsuUseCase
import de.crysxd.octoapp.preprintcontrols.ui.PrePrintControlsViewModel
import de.crysxd.octoapp.preprintcontrols.ui.widget.move.MoveToolWidgetViewModel
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