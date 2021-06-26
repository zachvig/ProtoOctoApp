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
import de.crysxd.octoapp.signin.discover.DiscoverViewModel
import de.crysxd.octoapp.signin.manual.ManualWebUrlViewModel
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
        octoPrintRepository: OctoPrintRepository
    ): ViewModel = DiscoverViewModel(
        discoverOctoPrintUseCase = discoverOctoPrintUseCase,
        octoPrintRepository = octoPrintRepository
    )

    @Provides
    @IntoMap
    @ViewModelKey(ManualWebUrlViewModel::class)
    open fun provideManualWebUrlViewModel(
        sensitiveDataMask: SensitiveDataMask,
    ): ViewModel = ManualWebUrlViewModel(
        sensitiveDataMask = sensitiveDataMask
    )
}