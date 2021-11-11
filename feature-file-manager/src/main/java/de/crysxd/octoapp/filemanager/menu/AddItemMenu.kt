package de.crysxd.octoapp.filemanager.menu

import android.content.Context
import android.net.Uri
import android.text.InputType
import androidx.lifecycle.asFlow
import de.crysxd.baseui.common.enter_value.EnterValueFragment
import de.crysxd.baseui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.CreateFolderUseCase
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.di.FileManagerInjector
import de.crysxd.octoapp.filemanager.upload.PickFileForUploadFragmentArgs
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize


@Parcelize
class AddItemMenu(private val origin: FileOrigin, private val folder: FileObject.Folder?) : Menu {

    override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___add_menu___title)

    override suspend fun getMenuItem() = listOf(
        FileActionsMenu.EnableFileManagementMenuItem(),
        AddFolderMenuItem(origin, folder),
        UploadFileMenuItem(origin, folder)
    )

    class AddFolderMenuItem(private val origin: FileOrigin, private val parent: FileObject.Folder?) : MenuItem {
        override val itemId = "create_folder"
        override var groupId = ""
        override val order = 101
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_create_new_folder_24
        override val canBePinned = false
        override val isEnabled get() = BillingManager.isFeatureEnabled(BillingManager.FEATURE_FILE_MANAGEMENT)

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___add_menu___create_folder_title)

        override suspend fun onClicked(host: MenuHost?) {
            val result = NavigationResultMediator.registerResultCallback<String>()
            val navController = host?.getNavController() ?: return
            val context = host.getMenuActivity()

            navController.navigate(
                de.crysxd.baseui.R.id.action_enter_value,
                EnterValueFragmentArgs(
                    title = context.getString(R.string.file_manager___add_menu___create_folder_title),
                    hint = context.getString(R.string.file_manager___add_menu___create_folder_input_hint),
                    action = context.getString(R.string.file_manager___add_menu___create_folder_action),
                    resultId = result.first,
                    inputType = InputType.TYPE_CLASS_TEXT,
                    validator = EnterValueFragment.NotEmptyValidator()
                ).toBundle()
            )

            val name = result.second.asFlow().first()?.takeIf { it.isNotEmpty() } ?: return host.closeMenu()
            BaseInjector.get().createFolderUseCase().execute(
                CreateFolderUseCase.Params(
                    parent = parent,
                    origin = origin,
                    name = name
                )
            )

            host.closeMenu()
        }
    }

    class UploadFileMenuItem(private val origin: FileOrigin, private val parent: FileObject.Folder?) : MenuItem {
        override val itemId = "add_file"
        override var groupId = ""
        override val order = 102
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_upload_file_24
        override val canBePinned = false
        override val isEnabled get() = BillingManager.isFeatureEnabled(BillingManager.FEATURE_FILE_MANAGEMENT)

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___add_menu___upload_file_title)

        override suspend fun onClicked(host: MenuHost?) {
            // Let user pick file
            val result = NavigationResultMediator.registerResultCallback<Uri>()
            host?.getNavController()?.navigate(
                R.id.action_pick_file_for_upload, PickFileForUploadFragmentArgs(
                    origin = origin,
                    parent = parent,
                    resultId = result.first
                ).toBundle()
            )

            // Start upload once ready
            result.second.asFlow().first()?.let {
                FileManagerInjector.get().uploadMediator().startUpload(
                    contentResolverUri = it,
                    origin = origin,
                    parent = parent
                )

            }

            host?.closeMenu()
        }
    }
}