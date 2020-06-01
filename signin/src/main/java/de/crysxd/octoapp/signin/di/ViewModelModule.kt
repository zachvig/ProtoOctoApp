package de.crysxd.octoapp.signin.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.signin.ui.SignInViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    @IntoMap
    @ViewModelKey(SignInViewModel::class)
    open fun provideSignInViewModel(
        validateSignInInformationUseCase: de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase,
        signInUseCase: de.crysxd.octoapp.signin.usecases.SignInUseCase
    ): ViewModel =
        SignInViewModel(validateSignInInformationUseCase, signInUseCase)

}