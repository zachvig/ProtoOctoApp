package de.crysxd.octoapp.base.di

import android.app.Application
import de.crysxd.octoapp.base.di.AndroidModule
import de.crysxd.octoapp.base.di.DaggerBaseComponent

object Injector {

    private lateinit var instance: BaseComponent

    fun init(app: Application) {
        val baseComponent = DaggerBaseComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

        instance = DaggerBaseComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

    }

    fun get(): BaseComponent = instance

}