package de.crysxd.octoapp.signin.di

import de.crysxd.octoapp.base.di.BaseComponent

object SignInInjector {

    private lateinit var instance: SignInComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerSignInComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): SignInComponent =
        instance

}