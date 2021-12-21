package de.crysxd.baseui.timelapse

import android.content.Context
import androidx.core.net.toUri
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.ConfirmedMenuItem
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.usecase.DownloadAndShareTimelapseUseCase
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseFile
import kotlinx.parcelize.Parcelize

@Parcelize
class TimelapseArchiveMenu(private val timelapseFile: TimelapseFile) : Menu {

    override fun shouldLoadBlocking() = true

    override suspend fun getMenuItem() = listOf(
        DeleteTimelapse(timelapseFile),
        PlayTimelapseMenuItem(timelapseFile),
        ShareTimelapseMenuItem(timelapseFile),
    )

    override suspend fun getTitle(context: Context) = timelapseFile.name
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.timelapse_archive___menu___subtitle, timelapseFile.bytes.asStyleFileSize())

    class DeleteTimelapse(private val file: TimelapseFile) : ConfirmedMenuItem() {
        override val itemId = "delete_timelapse"
        override var groupId = ""
        override val order = 1
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_delete_24

        override fun getConfirmMessage(context: Context) = context.getString(R.string.file_manager___file_menu___delete_confirmation_message, file.name)
        override fun getConfirmPositiveAction(context: Context) = getTitle(context)
        override fun getTitle(context: Context) = context.getString(R.string.timelapse_archive___menu___delete)
        override suspend fun onConfirmed(host: MenuHost?) {
            BaseInjector.get().timelapseRepository().delete(file)
            host?.closeMenu()
        }
    }

    class PlayTimelapseMenuItem(private val file: TimelapseFile) : MenuItem {
        override val itemId = "play_timelapse"
        override var groupId = ""
        override val order = 3
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_play_arrow_24

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_archive___menu___play)
        override suspend fun onClicked(host: MenuHost?) {
            val uri = requireNotNull(BaseInjector.get().timelapseRepository().download(file)?.toUri()) { "Failed to get file" }
            host?.getNavController()?.navigate(TimelapseArchiveFragmentDirections.actionPlayTimelapse(uri))
        }
    }

    class ShareTimelapseMenuItem(private val file: TimelapseFile) : MenuItem {
        override val itemId = "share_timelapse"
        override var groupId = ""
        override val order = 2
        override val canBePinned = false
        override val style = MenuItemStyle.OctoPrint
        override val icon = R.drawable.ic_round_share_24

        override fun getTitle(context: Context) = context.getString(R.string.timelapse_archive___menu___share)
        override suspend fun onClicked(host: MenuHost?) {
            host?.requireContext()?.let {
                BaseInjector.get().downloadAndShareTimelapseUseCase().execute(
                    DownloadAndShareTimelapseUseCase.Params(context = it, file = file)
                )
            }
            host?.closeMenu()
        }
    }
}
