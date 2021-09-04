package de.crysxd.octoapp

import de.crysxd.octoapp.base.di.Injector

fun initializeDagger() {
    de.crysxd.octoapp.signin.di.Injector.init(Injector.get())
    de.crysxd.octoapp.connect_printer.di.Injector.init(Injector.get())
    de.crysxd.octoapp.pre_print_controls.di.Injector.init(Injector.get())
    de.crysxd.octoapp.print_controls.di.Injector.init(Injector.get())
    de.crysxd.octoapp.filemanager.di.Injector.init(Injector.get())
    de.crysxd.octoapp.help.di.Injector.init(Injector.get())
}