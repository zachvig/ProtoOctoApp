package de.crysxd.octoapp.signin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintRepository
import de.crysxd.octoapp.signin.usecases.SignInUseCase
import de.crysxd.octoapp.signin.usecases.VerifySignInInformationUseCase

@Module
open class UseCaseModule {

    @Provides
    open fun provideSignInUseCase(octoprintRepository: OctoPrintRepository) =
        SignInUseCase(octoprintRepository)

    @Provides
    open fun provideVerifySignInInformationUseCase(context: Context) =
        VerifySignInInformationUseCase(context)

}