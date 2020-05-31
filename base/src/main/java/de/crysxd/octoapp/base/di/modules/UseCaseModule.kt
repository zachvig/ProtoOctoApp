package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase

@Module
class UseCaseModule {

    @Provides
    fun provideCheckOctoPrintInstanceInformationUseCase() =
        CheckOctoPrintInstanceInformationUseCase()

}