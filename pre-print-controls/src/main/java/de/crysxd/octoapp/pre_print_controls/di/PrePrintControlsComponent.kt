package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.BaseComponent

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
interface PrePrintControlsComponent {

    // ViewModelModule
    fun viewModelFactory(): ViewModelProvider.Factory

    // OctoprintModule
    fun octoprintProvider(): OctoPrintProvider

    // PicassoModule
    fun picasso(): LiveData<Picasso>

}