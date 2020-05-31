package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.BaseComponent

@PrePrintControlsScope
@Component(
    modules = [
        ViewModelModule::class
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface PrePrintControlsComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider() : OctoPrintProvider

}