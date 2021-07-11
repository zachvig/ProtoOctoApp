package de.crysxd.octoapp.help.di

import de.crysxd.octoapp.base.di.BaseComponent

object Injector {

    private lateinit var instance: HelpComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerHelpComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): HelpComponent = instance

}