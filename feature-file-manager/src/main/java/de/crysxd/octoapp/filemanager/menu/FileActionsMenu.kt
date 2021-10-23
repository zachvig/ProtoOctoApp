package de.crysxd.octoapp.filemanager.menu

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import de.crysxd.baseui.common.enter_value.EnterValueFragment
import de.crysxd.baseui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.baseui.menu.ConfirmedMenuItem
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.MoveFileUseCase
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize

@Parcelize
class FileActionsMenu(val file: FileObject) : Menu {

    override suspend fun getTitle(context: Context) = file.display

    override suspend fun getMenuItem() = listOf(
        DeleteFileMenuItem(file),
        RenameFileMenuItem(file)
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

    class RenameFileMenuItem(val file: FileObject) : MenuItem {
        override val itemId = "rename"
        override var groupId = ""
        override val order = 1
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_edit_24
        override val canBePinned = false

        override suspend fun getTitle(context: Context) = "Rename**"
        override suspend fun onClicked(host: MenuHost?) {
            val result = NavigationResultMediator.registerResultCallback<String>()
            val navController = host?.getNavController() ?: return
            val extension = file.name.split(".").takeIf { it.size > 1 }?.lastOrNull() ?: ""
            val originalName = file.name.removeSuffix(extension).removeSuffix(".")

            navController.navigate(
                de.crysxd.baseui.R.id.action_enter_value,
                EnterValueFragmentArgs(
                    title = "Rename ${file.name}**",
                    hint = "File name**",
                    action = "Rename**",
                    selectAll = true,
                    value = originalName,
                    resultId = result.first,
                    inputType = InputType.TYPE_CLASS_TEXT,
                    validator = EnterValueFragment.NotEmptyValidator()
                ).toBundle()
            )

            val name = result.second.asFlow().first()?.takeIf { it.isNotEmpty() } ?: return
            val newPath = file.path.removeSuffix(file.name) + name + (extension.takeIf { it.isNotEmpty() }?.let { ".$it" } ?: "")
            if (file.path != newPath) {
                BaseInjector.get().moveFileUseCase().execute(MoveFileUseCase.Params(file = file, newPath = newPath))
            }

            host.closeMenu()
        }
    }
}
