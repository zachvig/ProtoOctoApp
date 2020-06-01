package de.crysxd.octoapp.base.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.ui.temperature.ControlTemperatureViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(ControlTemperatureViewModel::class)
    open fun provideSignInViewModel(octoPrintProvider: OctoPrintProvider): ViewModel =
        ControlTemperatureViewModel(octoPrintProvider)

}