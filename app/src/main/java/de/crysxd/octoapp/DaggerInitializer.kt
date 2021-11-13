package de.crysxd.octoapp

import de.crysxd.baseui.di.BaseUiInjector
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.connectprinter.di.ConnectPrinterInjector
import de.crysxd.octoapp.filemanager.di.FileManagerInjector
import de.crysxd.octoapp.help.di.HelpInjector
import de.crysxd.octoapp.preprintcontrols.di.PrePrintControlsInjector
import de.crysxd.octoapp.printcontrols.di.PrintControlsInjector
import de.crysxd.octoapp.signin.di.SignInInjector

fun initializeDagger() {
    BaseUiInjector.init(BaseInjector.get())
    SignInInjector.init(BaseInjector.get())
    ConnectPrinterInjector.init(BaseInjector.get())
    PrePrintControlsInjector.init(BaseInjector.get())
    PrintControlsInjector.init(BaseInjector.get())
    FileManagerInjector.init(BaseInjector.get(), BaseUiInjector.get())
    HelpInjector.init(BaseInjector.get())
}