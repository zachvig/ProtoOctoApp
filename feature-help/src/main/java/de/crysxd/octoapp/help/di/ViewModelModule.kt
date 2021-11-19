package de.crysxd.octoapp.help.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.repository.TutorialsRepository
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.base.usecase.GetWebcamSettingsUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.help.plugins.PluginsLibraryViewModel
import de.crysxd.octoapp.help.troubleshoot.WebcamTroubleShootingViewModel
import de.crysxd.octoapp.help.tutorials.TutorialsViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(WebcamTroubleShootingViewModel::class)
    open fun provideWebcamTroubleShootingViewModel(
        octoPrintProvider: OctoPrintProvider,
        octoPrintRepository: OctoPrintRepository,
        getWebcamSettingsUseCase: GetWebcamSettingsUseCase,
        testFullNetworkStackUseCase: TestFullNetworkStackUseCase,
    ): ViewModel = WebcamTroubleShootingViewModel(
        octoPrintProvider = octoPrintProvider,
        octoPrintRepository = octoPrintRepository,
        getWebcamSettingsUseCase = getWebcamSettingsUseCase,
        testFullNetworkStackUseCase = testFullNetworkStackUseCase
    )

    @Provides
    @IntoMap
    @ViewModelKey(TutorialsViewModel::class)
    open fun provideTutorialsViewModel(
        tutorialsRepository: TutorialsRepository,
    ): ViewModel = TutorialsViewModel(
        tutorialsRepository = tutorialsRepository,
    )

    @Provides
    @IntoMap
    @ViewModelKey(PluginsLibraryViewModel::class)
    open fun providePluginsLibraryViewModel(): ViewModel = PluginsLibraryViewModel()
}