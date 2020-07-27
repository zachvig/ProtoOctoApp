package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.pre_print_controls.ui.PrePrintControlsViewModel
import de.crysxd.octoapp.pre_print_controls.ui.select_file.SelectFileViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.extrude.ExtrudeWidgetViewModel
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
        octoPrintProvider: OctoPrintProvider,
        turnOffPsuUseCase: TurnOffPsuUseCase,
        changeFilamentUseCase: ChangeFilamentUseCase
    ): ViewModel = PrePrintControlsViewModel(octoPrintProvider, turnOffPsuUseCase, changeFilamentUseCase)

    @Provides
    @IntoMap
    @ViewModelKey(MoveToolWidgetViewModel::class)
    open fun provideMoveToolControlsViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase1: HomePrintHeadUseCase,
        useCase2: JogPrintHeadUseCase
    ): ViewModel = MoveToolWidgetViewModel(useCase1, useCase2, octoPrintProvider)

    @Provides
    @IntoMap
    @ViewModelKey(ExtrudeWidgetViewModel::class)
    open fun provideExtrudeWidgetViewModel(
        octoPrintProvider: OctoPrintProvider,
        extrudeFilamentUseCase: ExtrudeFilamentUseCase
    ): ViewModel = ExtrudeWidgetViewModel(octoPrintProvider, extrudeFilamentUseCase)

    @Provides
    @IntoMap
    @ViewModelKey(SelectFileViewModel::class)
    open fun provideSelectFileViewModel(
        octoPrintProvider: OctoPrintProvider,
        loadFilesUseCase: LoadFilesUseCase,
        startPrintJobUseCase: StartPrintJobUseCase,
        picasso: LiveData<Picasso?>
    ): ViewModel = SelectFileViewModel(octoPrintProvider, loadFilesUseCase, startPrintJobUseCase, picasso)

}