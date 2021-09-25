package de.crysxd.octoapp.printcontrols.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.network.OctoPrintProvider

@PrintControlsScope
@Component(
    modules = [
        ViewModelModule::class
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface PrintControlsComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider() : OctoPrintProvider

}