package de.crysxd.octoapp.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.octoapp.ui.ViewModelFactory
import de.crysxd.usecases.di.UseCaseModule

@Component(
    modules = [
        AndroidModule::class,
        UseCaseModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent {

    // AndroidModule
    fun context(): Context
    fun app(): Application

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

}