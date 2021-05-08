package de.crysxd.octoapp.filemanager.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.repository.OctoPrintRepository

@PrePrintControlsScope
@Component(
    modules = [
        ViewModelModule::class,
        PicassoModule::class
    ],
    dependencies = [
        BaseComponent::class
    ]
)
interface FileManagerComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider(): OctoPrintProvider
    fun octoprintRepository(): OctoPrintRepository

    // PicassoModule
    fun picasso(): LiveData<Picasso>

}