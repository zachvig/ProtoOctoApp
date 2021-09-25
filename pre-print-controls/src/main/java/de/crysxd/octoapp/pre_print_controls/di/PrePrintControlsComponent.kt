package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.network.OctoPrintProvider

@PrePrintControlsScope
@Component(
    modules = [
        ViewModelModule::class,
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface PrePrintControlsComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider(): OctoPrintProvider
    fun octoprintRepository(): OctoPrintRepository
}