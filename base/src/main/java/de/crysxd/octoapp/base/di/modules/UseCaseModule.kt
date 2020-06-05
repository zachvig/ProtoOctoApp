package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.usecase.*

@Module
class UseCaseModule {

    @Provides
    fun provideCheckOctoPrintInstanceInformationUseCase() =
        CheckOctoPrintInstanceInformationUseCase()

    @Provides
    fun provideSetToolTargetTemperatureUseCase() =
        SetToolTargetTemperatureUseCase()

    @Provides
    fun provideSetBedTargetTemperatureUseCase() =
        SetBedTargetTemperatureUseCase()

    @Provides
    fun provideHomePrintHeadUseCase() =
        HomePrintHeadUseCase()

    @Provides
    fun provideJogPrintHeadUseCase() =
        JogPrintHeadUseCase()

    @Provides
    fun provideTurnOnPsuUseCase() =
        TurnOnPsuUseCase()

    @Provides
    fun provideTurnOffPsuUseCase() =
        TurnOffPsuUseCase()

}