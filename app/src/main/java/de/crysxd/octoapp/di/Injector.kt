package de.crysxd.octoapp.di

import android.app.Application

object Injector {

    private lateinit var instance: AppComponent

    fun init(app: Application) {
        instance = DaggerAppComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

    }

    fun get(): AppComponent = instance

}