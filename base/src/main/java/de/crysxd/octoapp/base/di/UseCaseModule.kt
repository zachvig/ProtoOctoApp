package de.crysxd.octoapp.base.di

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase

@Module
class UseCaseModule {

    @Provides
    fun provideCheckOctoPrintInstanceInformationUseCase() =
        CheckOctoPrintInstanceInformationUseCase()

}