package de.crysxd.octoapp.signin.di

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.signin.troubleshoot.TroubleShootViewModel
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
        sharedPreferences: SharedPreferences
    ): ViewModel = SignInViewModel(
        octoPrintRepository,
        validateSignInInformationUseCase,
        signInUseCase,
        sharedPreferences
    )

    @Provides
    @IntoMap
    @ViewModelKey(TroubleShootViewModel::class)
    open fun provideTroubleShootViewModel(): ViewModel =
        TroubleShootViewModel()

}