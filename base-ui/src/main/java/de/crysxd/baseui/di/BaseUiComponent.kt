package de.crysxd.baseui.di

import dagger.Component
import de.crysxd.baseui.BaseViewModelFactory
import de.crysxd.octoapp.base.di.BaseComponent

@BaseUiScope
@Component(
    dependencies = [
        BaseComponent::class,
    ],
    modules = [
        ViewModelModule::class,

    ]
)
interface BaseUiComponent {

    // ViewModelModule
    fun viewModelFactory(): BaseViewModelFactory

}