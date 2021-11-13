package de.crysxd.baseui.di

import androidx.lifecycle.LiveData
import com.squareup.picasso.Picasso
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
        PicassoModule::class,
    ]
)
interface BaseUiComponent {

    // ViewModelModule
    fun viewModelFactory(): BaseViewModelFactory

    // PicassoModule
    fun picasso(): LiveData<Picasso>
}