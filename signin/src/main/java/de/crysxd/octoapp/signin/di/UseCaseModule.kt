package de.crysxd.octoapp.signin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.signin.usecases.SignInUseCase
import de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase

@Module
open class UseCaseModule {

    @Provides
    open fun provideSignInUseCase(context: Context) =
        SignInUseCase(context)

    @Provides
    open fun provideVerifySignInInformationUseCase(context: Context) =
        VerifySignInInformationUseCase(context)

}