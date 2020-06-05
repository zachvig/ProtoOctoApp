package de.crysxd.octoapp.base.di.modules

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.BaseViewModelFactory
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueViewModel
import de.crysxd.octoapp.base.ui.temperature.ControlBedTemperatureViewModel
import de.crysxd.octoapp.base.ui.temperature.ControlToolTemperatureViewModel
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): BaseViewModelFactory =
        BaseViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(ControlToolTemperatureViewModel::class)
    open fun provideControlToolTemperatureViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase: SetToolTargetTemperatureUseCase
    ): ViewModel = ControlToolTemperatureViewModel(octoPrintProvider, useCase)

    @Provides
    @IntoMap
    @ViewModelKey(ControlBedTemperatureViewModel::class)
    open fun provideControlBedTemperatureViewModel(
        octoPrintProvider: OctoPrintProvider,
        useCase: SetBedTargetTemperatureUseCase
    ): ViewModel = ControlBedTemperatureViewModel(octoPrintProvider, useCase)


    @Provides
    @IntoMap
    @ViewModelKey(EnterValueViewModel::class)
    open fun provideEnterValueViewModel(): ViewModel = EnterValueViewModel()

}