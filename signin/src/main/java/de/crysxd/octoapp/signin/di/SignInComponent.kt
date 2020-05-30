package de.crysxd.octoapp.signin.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.base.di.AndroidModule

@Component(
    modules = [
        AndroidModule::class,
        UseCaseModule::class,
        ViewModelModule::class
    ],
    dependencies = [
    ]
)
interface SignInComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

}