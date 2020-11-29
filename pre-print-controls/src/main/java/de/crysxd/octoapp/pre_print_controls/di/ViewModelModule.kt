package de.crysxd.octoapp.pre_print_controls.di

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.repository.GcodeFileRepository
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.usecase.*
import de.crysxd.octoapp.pre_print_controls.ui.PrePrintControlsViewModel
import de.crysxd.octoapp.pre_print_controls.ui.file_details.FileDetailsViewModel
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
        octoPrintProvider: OctoPrintProvider,
        useCase1: HomePrintHeadUseCase,
        useCase2: JogPrintHeadUseCase
    ): ViewModel = MoveToolWidgetViewModel(
        useCase1,
        useCase2,
        octoPrintProvider
    )

    @Provides
    @IntoMap
    @ViewModelKey(ExtrudeWidgetViewModel::class)
    open fun provideExtrudeWidgetViewModel(
        setToolTargetTemperatureUseCase: SetToolTargetTemperatureUseCase,
        extrudeFilamentUseCase: ExtrudeFilamentUseCase
    ): ViewModel = ExtrudeWidgetViewModel(
        extrudeFilamentUseCase,
        setToolTargetTemperatureUseCase
    )

    @Provides
    @IntoMap
    @ViewModelKey(SelectFileViewModel::class)
    open fun provideSelectFileViewModel(
        loadFilesUseCase: LoadFilesUseCase,
        startPrintJobUseCase: StartPrintJobUseCase,
        sharedPreferences: SharedPreferences,
        picasso: LiveData<Picasso?>
    ): ViewModel = SelectFileViewModel(
        loadFilesUseCase,
        startPrintJobUseCase,
        sharedPreferences,
        picasso
    )

    @Provides
    @IntoMap
    @ViewModelKey(FileDetailsViewModel::class)
    open fun provideFileDetailsViewModel(
        startPrintJobUseCase: StartPrintJobUseCase,
        getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
        generateRenderStyleUseCase: GenerateRenderStyleUseCase,
        gcodeFileRepository: GcodeFileRepository,
    ): ViewModel = FileDetailsViewModel(
        getCurrentPrinterProfileUseCase,
        generateRenderStyleUseCase,
        startPrintJobUseCase,
        gcodeFileRepository
    )
}