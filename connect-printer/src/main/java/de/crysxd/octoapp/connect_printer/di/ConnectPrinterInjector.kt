package de.crysxd.octoapp.connect_printer.di

import de.crysxd.octoapp.base.di.BaseComponent

object ConnectPrinterInjector {

    private lateinit var instance: ConnectPrinterComponent

    fun init(baseComponent: BaseComponent) {
        instance = DaggerConnectPrinterComponent.builder()
            .baseComponent(baseComponent)
            .build()
    }

    fun get(): ConnectPrinterComponent = instance

}