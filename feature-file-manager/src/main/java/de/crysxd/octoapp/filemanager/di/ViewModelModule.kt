package de.crysxd.octoapp.filemanager.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.LoadFileUseCase
import de.crysxd.octoapp.base.usecase.LoadFilesUseCase
import de.crysxd.octoapp.base.usecase.MoveFileUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.filemanager.ui.file_details.FileDetailsViewModel
import de.crysxd.octoapp.filemanager.ui.select_file.MoveAndCopyFilesViewModel
import de.crysxd.octoapp.filemanager.ui.select_file.SelectFileViewModel
import de.crysxd.octoapp.filemanager.upload.UploadMediator
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(SelectFileViewModel::class)
    open fun provideSelectFileViewModel(
        loadFilesUseCase: LoadFilesUseCase,
        loadFileUseCase: LoadFileUseCase,
        octoPreferences: OctoPreferences,
        moveFileUseCase: MoveFileUseCase,
        octoPrintProvider: OctoPrintProvider,
        uploadMediator: UploadMediator,
        picasso: LiveData<Picasso?>
    ): ViewModel = SelectFileViewModel(
        loadFilesUseCase = loadFilesUseCase,
        loadFileUseCase = loadFileUseCase,
        moveFileUseCase = moveFileUseCase,
        octoPreferences = octoPreferences,
        octoPrintProvider = octoPrintProvider,
        picasso = picasso,
        uploadMediator = uploadMediator,
    )

    @Provides
    @IntoMap
    @ViewModelKey(FileDetailsViewModel::class)
    open fun provideFileDetailsViewModel(
        startPrintJobUseCase: StartPrintJobUseCase,
        octoPrintProvider: OctoPrintProvider,
    ): ViewModel = FileDetailsViewModel(
        startPrintJobUseCase = startPrintJobUseCase,
        octoPrintProvider = octoPrintProvider
    )

    @Provides
    @IntoMap
    @ViewModelKey(MoveAndCopyFilesViewModel::class)
    open fun provideMoveAndCopyFilesViewModel(): ViewModel = MoveAndCopyFilesViewModel()
}