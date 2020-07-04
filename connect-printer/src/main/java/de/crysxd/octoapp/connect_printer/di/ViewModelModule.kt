package de.crysxd.octoapp.connect_printer.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.ui.ViewModelFactory
import de.crysxd.octoapp.base.usecase.AutoConnectPrinterUseCase
import de.crysxd.octoapp.base.usecase.GetPrinterConnectionUseCase
import de.crysxd.octoapp.base.usecase.TurnOnPsuUseCase
import de.crysxd.octoapp.connect_printer.ui.ConnectPrinterViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(ConnectPrinterViewModel::class)
    open fun provideSignInViewModel(
        octoPrintProvider: OctoPrintProvider,
        turnOnPsuUseCase: TurnOnPsuUseCase,
        autoConnectPrinterUseCase: AutoConnectPrinterUseCase,
        getPrinterConnectionUseCase: GetPrinterConnectionUseCase
    ): ViewModel = ConnectPrinterViewModel(
        octoPrintProvider,
        turnOnPsuUseCase,
        autoConnectPrinterUseCase,
        getPrinterConnectionUseCase
    )

}