package de.crysxd.octoapp.pre_print_controls.di

import de.crysxd.octoapp.base.di.BaseComponent

object PrePrintControlsInjector {

    private lateinit var instance: PrePrintControlsComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerPrePrintControlsComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): PrePrintControlsComponent = instance

}