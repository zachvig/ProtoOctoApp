package de.crysxd.octoapp.connectprinter.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.network.OctoPrintProvider

@ConnectPrinterScope
@Component(
    modules = [
        ViewModelModule::class
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface ConnectPrinterComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider() : OctoPrintProvider

}