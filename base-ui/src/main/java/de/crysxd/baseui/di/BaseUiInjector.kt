package de.crysxd.baseui.di

import androidx.annotation.VisibleForTesting
import de.crysxd.octoapp.base.di.BaseComponent

object BaseUiInjector {

    private lateinit var instance: BaseUiComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerBaseUiComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): BaseUiComponent = instance

    @VisibleForTesting
    fun set(component: BaseUiComponent) {
        instance = component
    }
}