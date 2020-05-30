package de.crysxd.octoapp.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.ui.ViewModelFactory
import de.crysxd.octoapp.ui.signin.SignInViewModel
import de.crysxd.usecases.signin.SignInUseCase
import de.crysxd.usecases.signin.VerifySignInInformationUseCase
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
        validateSignInInformationUseCase: VerifySignInInformationUseCase,
        signInUseCase: SignInUseCase
    ): ViewModel =
        SignInViewModel(validateSignInInformationUseCase, signInUseCase)

}