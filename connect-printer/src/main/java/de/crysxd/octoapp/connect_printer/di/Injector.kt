package de.crysxd.octoapp.connect_printer.di

import android.app.Application
import de.crysxd.octoapp.base.di.AndroidModule
import de.crysxd.octoapp.base.di.DaggerBaseComponent

object Injector {

    private lateinit var instance: ConnectPrinterComponent

    fun init(app: Application) {
        val baseComponent = DaggerBaseComponent.builder()
            .androidModule(AndroidModule(app))
            .build()

        instance = DaggerConnectPrinterComponent.builder()
            .baseComponent(baseComponent)
            .build()

    }

    fun get(): ConnectPrinterComponent = instance

}