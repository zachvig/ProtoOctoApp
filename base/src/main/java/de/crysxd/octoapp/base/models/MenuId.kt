package de.crysxd.octoapp.base.models

sealed class MenuId {
    object MainMenu : MenuId()
    object PrintWorkspace : MenuId()
    object PrepareWorkspace : MenuId()
}