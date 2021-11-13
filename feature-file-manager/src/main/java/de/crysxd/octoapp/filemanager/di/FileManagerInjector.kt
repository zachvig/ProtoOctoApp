package de.crysxd.octoapp.filemanager.di

import de.crysxd.baseui.di.BaseUiComponent
import de.crysxd.octoapp.base.di.BaseComponent

object FileManagerInjector {

    private lateinit var instance: FileManagerComponent

    fun init(baseComponent: BaseComponent, baseUiComponent: BaseUiComponent) {
        instance = DaggerFileManagerComponent.builder()
            .baseComponent(baseComponent)
            .baseUiComponent(baseUiComponent)
            .build()
    }

    fun get(): FileManagerComponent = instance

}