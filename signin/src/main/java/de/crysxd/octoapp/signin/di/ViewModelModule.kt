package de.crysxd.octoapp.signin.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.signin.discover.DiscoverViewModel
import de.crysxd.octoapp.signin.probe.ProbeOctoPrintViewModel
import de.crysxd.octoapp.signin.ui.SignInViewModel
import de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(SignInViewModel::class)
    open fun provideSignInViewModel(
        octoPrintRepository: OctoPrintRepository,
        validateSignInInformationUseCase: VerifySignInInformationUseCase,
        signInUseCase: de.crysxd.octoapp.signin.usecases.SignInUseCase,
        sensitiveDataMask: SensitiveDataMask
    ): ViewModel = SignInViewModel(
        octoPrintRepository,
        validateSignInInformationUseCase,
        signInUseCase,
        sensitiveDataMask
    )

    @Provides
    @IntoMap
    @ViewModelKey(DiscoverViewModel::class)
    open fun provideDiscoverViewModel(
        discoverOctoPrintUseCase: DiscoverOctoPrintUseCase,
        octoPrintRepository: OctoPrintRepository,
        sensitiveDataMask: SensitiveDataMask,
    ): ViewModel = DiscoverViewModel(
        discoverOctoPrintUseCase = discoverOctoPrintUseCase,
        octoPrintRepository = octoPrintRepository,
        sensitiveDataMask = sensitiveDataMask
    )

    @Provides
    @IntoMap
    @ViewModelKey(ProbeOctoPrintViewModel::class)
    open fun provideProbeOctoPrintViewModel(
        useCase: TestFullNetworkStackUseCase
    ): ViewModel = ProbeOctoPrintViewModel(
        useCase = useCase
    )
}