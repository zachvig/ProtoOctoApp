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
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.usecase.DownloadAndShareFileUseCase
import de.crysxd.octoapp.base.usecase.MoveFileUseCase
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.di.injectActivityViewModel
import de.crysxd.octoapp.filemanager.ui.select_file.MoveAndCopyFilesViewModel
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize

@Parcelize
class FileActionsMenu(val file: FileObject) : Menu {

    override suspend fun getTitle(context: Context) = file.display

    override suspend fun getMenuItem() = listOfNotNull(
        DeleteFileMenuItem(file),
        RenameFileMenuItem(file),
        CopyFileMenuItem(file),
        MoveFileMenuItem(file),
        (file as? FileObject.File)?.let { DownloadAndShareMenuItem(it) }
    )

    class DeleteFileMenuItem(val file: FileObject) : ConfirmedMenuItem() {
        override val itemId = "delete"
        override var groupId = ""
        override val order = 2
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_delete_outline_24
        override val canBePinned = false

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___file_menu___delete)
        override fun getConfirmMessage(context: Context) = context.getString(R.string.file_manager___file_menu___delete, file.display)
        override fun getConfirmPositiveAction(context: Context) = context.getString(R.string.file_manager___file_menu___delete)
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

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___file_menu___rename)
        override suspend fun onClicked(host: MenuHost?) {
            val result = NavigationResultMediator.registerResultCallback<String>()
            val navController = host?.getNavController() ?: return
            val extension = (file as? FileObject.File)?.extension?.let { ".$it" } ?: ""
            val originalName = file.name.removeSuffix(extension)
            val context = host.requireContext()

            navController.navigate(
                de.crysxd.baseui.R.id.action_enter_value,
                EnterValueFragmentArgs(
                    title = context.getString(R.string.file_manager___file_menu___rename_input_title),
                    hint = context.getString(R.string.file_manager___file_menu___rename_input_hint),
                    action = context.getString(R.string.file_manager___file_menu___rename_action),
                    selectAll = true,
                    value = originalName,
                    resultId = result.first,
                    inputType = InputType.TYPE_CLASS_TEXT,
                    validator = EnterValueFragment.NotEmptyValidator()
                ).toBundle()
            )

            val name = result.second.asFlow().first()?.takeIf { it.isNotEmpty() } ?: return
            val newPath = file.path.removeSuffix(file.name) + name + extension
            if (file.path != newPath) {
                BaseInjector.get().moveFileUseCase().execute(MoveFileUseCase.Params(file = file, newPath = newPath))
            }

            host.closeMenu()
        }
    }

    class CopyFileMenuItem(val file: FileObject) : MenuItem {
        override val itemId = "copy"
        override var groupId = ""
        override val order = 3
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_content_copy_24
        override val canBePinned = false

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___file_menu___copy)
        override suspend fun onClicked(host: MenuHost?) {
            host?.getHostFragment()?.let {
                it.injectActivityViewModel<MoveAndCopyFilesViewModel>().value.let { vm ->
                    vm.copyFile = true
                    vm.selectedFile.value = file
                }
            }
            host?.closeMenu()
        }
    }

    class MoveFileMenuItem(val file: FileObject) : MenuItem {
        override val itemId = "move"
        override var groupId = ""
        override val order = 4
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_content_cut_24
        override val canBePinned = false

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___file_menu___move)
        override suspend fun onClicked(host: MenuHost?) {
            host?.getHostFragment()?.let {
                it.injectActivityViewModel<MoveAndCopyFilesViewModel>().value.let { vm ->
                    vm.copyFile = false
                    vm.selectedFile.value = file
                }
            }
            host?.closeMenu()
        }
    }

    class DownloadAndShareMenuItem(val file: FileObject.File) : MenuItem {
        override val itemId = "download"
        override var groupId = ""
        override val order = 5
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_share_24
        override val canBePinned = false

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___file_menu___download_and_share)
        override suspend fun getDescription(context: Context) =
            context.getString(R.string.file_manager___file_menu___download_and_share_description, file.size.asStyleFileSize())

        override suspend fun onClicked(host: MenuHost?) {
            host?.getMenuActivity()?.let {
                BaseInjector.get().downloadAndShareFileUseCase().execute(
                    DownloadAndShareFileUseCase.Params(context = it, file = file)
                )
            }
            host?.closeMenu()
        }
    }
}
