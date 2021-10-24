package de.crysxd.octoapp.filemanager.menu

import android.content.Context
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.RevolvingOptionsMenuItem
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.data.models.FileManagerSettings
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.filemanager.R
import kotlinx.parcelize.Parcelize

@Parcelize
class SortOptionsMenu : Menu {

    override suspend fun getMenuItem() = listOf(
        SortByMenuItem(BaseInjector.get().localizedContext()),
        SortDirectionMenuItem(BaseInjector.get().localizedContext()),
        HidePrintedMenuItem()
    )

    class SortByMenuItem(private val context: Context) : RevolvingOptionsMenuItem() {
        override val activeValue get() = BaseInjector.get().octoPreferences().fileManagerSettings.sortBy.name
        override val canBePinned = false
        override val itemId = "sort_by"
        override var groupId = "none"
        override val order = 1
        override val style = MenuItemStyle.Settings
        override val isEnabled = true
        override val icon: Int = R.drawable.ic_round_sort_24
        override val options = listOf(
            Option(
                label = context.getString(R.string.file_manager___sorting_menu___sort_by_upload_time),
                value = FileManagerSettings.SortBy.UploadTime.name
            ),
            Option(
                label = context.getString(R.string.file_manager___sorting_menu___sort_by_last_print_time),
                value = FileManagerSettings.SortBy.PrintTime.name
            ),
            Option(
                label = context.getString(R.string.file_manager___sorting_menu___sort_by_name),
                value = FileManagerSettings.SortBy.Name.name
            ),
            Option(
                label = context.getString(R.string.file_manager___sorting_menu___sort_by_file_size),
                value = FileManagerSettings.SortBy.FileSize.name
            ),
        )

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___sorting_menu___sort_by)

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            BaseInjector.get().octoPreferences().fileManagerSettings = BaseInjector.get().octoPreferences().fileManagerSettings.copy(
                sortBy = FileManagerSettings.SortBy.valueOf(option.value)
            )
        }
    }

    class SortDirectionMenuItem(private val context: Context) : RevolvingOptionsMenuItem() {

        override val activeValue get() = BaseInjector.get().octoPreferences().fileManagerSettings.sortDirection.name
        override val canBePinned = false
        override val itemId = "sort_directon"
        override var groupId = "none"
        override val order = 2
        override val style = MenuItemStyle.Settings
        override val isEnabled = true
        override val icon: Int = R.drawable.ic_round_swap_vert_24
        override val options = listOf(
            Option(
                label = context.getString(R.string.file_manager___sorting_menu___direction_ascending),
                value = FileManagerSettings.SortDirection.Ascending.name
            ),
            Option(
                label = context.getString(R.string.file_manager___sorting_menu___direction_decending),
                value = FileManagerSettings.SortDirection.Descending.name
            ),
        )

        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___sorting_menu___direction)

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            BaseInjector.get().octoPreferences().fileManagerSettings = BaseInjector.get().octoPreferences().fileManagerSettings.copy(
                sortDirection = FileManagerSettings.SortDirection.valueOf(option.value)
            )
        }
    }

    class HidePrintedMenuItem : ToggleMenuItem() {
        override val isEnabled get() = BaseInjector.get().octoPreferences().fileManagerSettings.hidePrintedFiles
        override val itemId = "hide_printed"
        override var groupId = "none"
        override val order = 3
        override val style = MenuItemStyle.Settings
        override val canBePinned = false
        override val icon: Int = R.drawable.ic_round_remove_done_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.file_manager___sorting_menu___hide_printed)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().fileManagerSettings = BaseInjector.get().octoPreferences().fileManagerSettings.copy(
                hidePrintedFiles = enabled
            )
        }
    }
}