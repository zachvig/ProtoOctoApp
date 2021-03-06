package de.crysxd.octoapp.pre_print_controls.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import dagger.Component
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.widget.OctoWidgetRecycler

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

    // BaseComponent
    fun octoWidgetRecycler(): OctoWidgetRecycler

    // OctoprintModule
    fun octoprintProvider(): OctoPrintProvider
    fun octoprintRepository(): OctoPrintRepository

    // PicassoModule
    fun picasso(): LiveData<Picasso>

}