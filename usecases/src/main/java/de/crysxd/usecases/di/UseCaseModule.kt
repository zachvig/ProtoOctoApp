package de.crysxd.usecases.di

import android.content.Context
import dagger.Module
import dagger.Provides
import de.crysxd.usecases.signin.SignInUseCase
import de.crysxd.usecases.signin.VerifySignInInformationUseCase

@Module
open class UseCaseModule {

    @Provides
    open fun provideSignInUseCase(context: Context) =
        SignInUseCase(context)

    @Provides
    open fun provideVerifySignInInformationUseCase(context: Context) =
        VerifySignInInformationUseCase(context)

}