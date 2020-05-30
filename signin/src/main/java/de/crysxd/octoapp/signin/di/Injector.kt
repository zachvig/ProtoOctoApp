package de.crysxd.octoapp.signin.di

import android.app.Application
import de.crysxd.octoapp.base.di.AndroidModule

object Injector {

    private lateinit var instance: SignInComponent

    fun init(app: Application) {
        instance = DaggerSignInComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

    }

    fun get(): SignInComponent = instance

}