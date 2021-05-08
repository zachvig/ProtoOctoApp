package de.crysxd.octoapp.filemanager.di

import de.crysxd.octoapp.base.di.BaseComponent

object Injector {

    private lateinit var instance: FileManagerComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerFileManagerComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): FileManagerComponent = instance

}