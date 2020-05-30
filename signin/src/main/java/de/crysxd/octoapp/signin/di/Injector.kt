package de.crysxd.octoapp.signin.di

import android.app.Application
import de.crysxd.octoapp.base.di.AndroidModule
import de.crysxd.octoapp.base.di.DaggerBaseComponent

object Injector {

    private lateinit var instance: SignInComponent

    fun init(app: Application) {
        val baseComponent = DaggerBaseComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

        instance = DaggerSignInComponent.builder()
            .baseComponent(baseComponent)
            .build()

    }

    fun get(): SignInComponent = instance

}