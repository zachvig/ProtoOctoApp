package de.crysxd.octoapp.help.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.di.BaseComponent

@HelpScope
@Component(
    modules = [
        ViewModelModule::class,
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface HelpComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

}