package de.crysxd.octoapp.base.di

import android.app.Application
import androidx.annotation.VisibleForTesting
import de.crysxd.octoapp.base.di.modules.AndroidModule

object Injector {

    private lateinit var instance: BaseComponent

    fun init(app: Application) {
        instance = DaggerBaseComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

    }

    fun get(): BaseComponent = instance

    @VisibleForTesting
    fun set(component: BaseComponent) {
        instance = component
    }
}