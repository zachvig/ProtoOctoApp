package de.crysxd.octoapp.filemanager.menu

import android.content.Context
import de.crysxd.baseui.menu.ConfirmedMenuItem
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.parcelize.Parcelize

@Parcelize
class FileActionsMenu(val file: FileObject) : Menu {

    override suspend fun getTitle(context: Context) = file.display

    override suspend fun getMenuItem() = listOf(
        DeleteFileMenuItem(file)
    )

    class DeleteFileMenuItem(val file: FileObject) : ConfirmedMenuItem() {
        override val itemId = "delete"
        override var groupId = ""
        override val order = 1
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_delete_24
        override val canBePinned = false

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_actions_menu___delete)
        override fun getConfirmMessage(context: Context) = context.getString(R.string.file_actions_menu___delete_confirmation_message, file.display)
        override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.file_action_menu___delete_confirmation_action)
        override suspend fun onConfirmed(host: MenuHost?) {
            BaseInjector.get().deleteFileUseCase().execute(file)
            host?.closeMenu()
        }
    }
}
