package de.crysxd.octoapp.filemanager.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import de.crysxd.baseui.di.BaseUiComponent
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.filemanager.upload.UploadMediator

@FileManagerScope
@Component(
    modules = [
        ViewModelModule::class,
    ],
    dependencies = [
        BaseComponent::class,
        BaseUiComponent::class,
    ]
)
interface FileManagerComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider(): OctoPrintProvider

    // Others
    fun uploadMediator(): UploadMediator
}