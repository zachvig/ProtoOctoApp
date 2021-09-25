package de.crysxd.octoapp.signin.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.BaseComponent

@SignInScope
@Component(
    modules = [
        ViewModelModule::class
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface SignInComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintRepository() : OctoPrintRepository

}