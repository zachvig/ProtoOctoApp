package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase

@Module
class UseCaseModule {

    @Provides
    fun provideCheckOctoPrintInstanceInformationUseCase() =
        CheckOctoPrintInstanceInformationUseCase()


    @Provides
    fun provideSetToolTargetTemperatureUseCase() =
        SetToolTargetTemperatureUseCase()

}