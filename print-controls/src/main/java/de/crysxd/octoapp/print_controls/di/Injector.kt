package de.crysxd.octoapp.print_controls.di

import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.pre_print_controls.di.DaggerPrintControlsComponent
import de.crysxd.octoapp.pre_print_controls.di.PrintControlsComponent

object Injector {

    private lateinit var instance: PrintControlsComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerPrintControlsComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): PrintControlsComponent = instance

}