package de.crysxd.octoapp.connect_printer.di

import android.app.Application
import de.crysxd.octoapp.base.di.AndroidModule
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.di.DaggerBaseComponent

object Injector {

    private lateinit var instance: ConnectPrinterComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerConnectPrinterComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): ConnectPrinterComponent = instance

}