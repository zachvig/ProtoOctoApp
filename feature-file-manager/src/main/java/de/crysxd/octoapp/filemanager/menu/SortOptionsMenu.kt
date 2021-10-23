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
        SortByMenuItem(),
        SortDirectionMenuItem(),
        HidePrintedMenuItem()
    )

    class SortByMenuItem : RevolvingOptionsMenuItem() {
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
                label = "Upload time**",
                value = FileManagerSettings.SortBy.UploadTime.name
            ),
            Option(
                label = "Last print time**",
                value = FileManagerSettings.SortBy.PrintTime.name
            ),
            Option(
                label = "Name**",
                value = FileManagerSettings.SortBy.Name.name
            ),
            Option(
                label = "File size**",
                value = FileManagerSettings.SortBy.FileSize.name
            ),
        )

        override suspend fun getTitle(context: Context) = "Sort by**"

        override fun handleOptionActivated(host: MenuHost?, option: Option) {
            BaseInjector.get().octoPreferences().fileManagerSettings = BaseInjector.get().octoPreferences().fileManagerSettings.copy(
                sortBy = FileManagerSettings.SortBy.valueOf(option.value)
            )
        }
    }

    class SortDirectionMenuItem : RevolvingOptionsMenuItem() {
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
                label = "Ascending**",
                value = FileManagerSettings.SortDirection.Ascending.name
            ),
            Option(
                label = "Descending**",
                value = FileManagerSettings.SortDirection.Descending.name
            ),
        )

        override suspend fun getTitle(context: Context) = "Direction**"

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
        override suspend fun getTitle(context: Context) = "Hide successfully printed**"

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().fileManagerSettings = BaseInjector.get().octoPreferences().fileManagerSettings.copy(
                hidePrintedFiles = enabled
            )
        }
    }
}