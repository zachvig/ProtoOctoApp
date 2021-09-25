package de.crysxd.octoapp.print_controls.di

import de.crysxd.octoapp.base.di.BaseComponent

object PrintControlsInjector {

    private lateinit var instance: PrintControlsComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerPrintControlsComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): PrintControlsComponent = instance

}