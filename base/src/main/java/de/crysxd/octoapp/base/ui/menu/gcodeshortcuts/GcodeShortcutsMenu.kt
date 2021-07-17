package de.crysxd.octoapp.base.ui.menu.gcodeshortcuts

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.utils.NavigationResultMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@Parcelize
class GcodeShortcutsMenu(private val command: GcodeHistoryItem, private val insertCallback: ((GcodeHistoryItem) -> Unit)? = null) : Menu {
    override fun shouldLoadBlocking() = true
    override suspend fun getTitle(context: Context) = command.name
    override suspend fun getSubtitle(context: Context) = if (command.name == command.oneLineCommand) null else command.oneLineCommand
    override suspend fun getMenuItem(): List<MenuItem> {
        val repo = Injector.get().gcodeHistoryRepository()
        return listOfNotNull(
            TogglePinnedMenuItem(repo, command),
            insertCallback?.let { InsertMenuItem(it, repo, command) },
            SetLabelMenuItem(repo, command),
            ClearLabelMenuItem(repo, command),
            RemoveMenuItem(repo, command)
        )
    }

    private abstract class GcodeShortcutMenuItem(protected val repo: GcodeHistoryRepository, protected val command: GcodeHistoryItem) : MenuItem {
        override val itemId = ""
        override var groupId = ""
        override val style = MenuItemStyle.Printer
        override val canBePinned = false
    }

    private class TogglePinnedMenuItem(repo: GcodeHistoryRepository, command: GcodeHistoryItem) : GcodeShortcutMenuItem(repo, command) {
        override val order = 1
        override val icon = R.drawable.ic_round_push_pin_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.toggle_favorite)
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            repo.setFavorite(command.command, !command.isFavorite)
            host?.dismissAllowingStateLoss()
        }
    }

    private class InsertMenuItem(private val callback: (GcodeHistoryItem) -> Unit, repo: GcodeHistoryRepository, command: GcodeHistoryItem) :
        GcodeShortcutMenuItem(repo, command) {
        override val order = 2
        override val icon = R.drawable.ic_round_content_paste_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.insert)
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            callback(command)
            host?.dismissAllowingStateLoss()
        }
    }

    private class SetLabelMenuItem(repo: GcodeHistoryRepository, command: GcodeHistoryItem) : GcodeShortcutMenuItem(repo, command) {
        override val order = 3
        override val icon = R.drawable.ic_round_label_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.enter_label)
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            host ?: return
            val result = NavigationResultMediator.registerResultCallback<String?>()
            val context = host.requireContext()

            host.findNavController().navigate(
                R.id.action_enter_value,
                EnterValueFragmentArgs(
                    title = context.getString(R.string.enter_label),
                    hint = context.getString(R.string.label_for_x, command.oneLineCommand),
                    action = context.getString(R.string.set_lebel),
                    resultId = result.first,
                    value = command.label,
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                    selectAll = true
                ).toBundle()
            )

            withContext(Dispatchers.Default) {
                result.second.asFlow().first()
            }?.let { label ->
                repo.setLabelForGcode(command = command.command, label = label)
                host.dismissAllowingStateLoss()
            }
        }
    }

    private class ClearLabelMenuItem(repo: GcodeHistoryRepository, command: GcodeHistoryItem) : GcodeShortcutMenuItem(repo, command) {
        override val order = 4
        override val icon = R.drawable.ic_round_label_off_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.clear_lebel)
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            repo.setLabelForGcode(command.command, null)
            host?.dismissAllowingStateLoss()
        }
    }

    private class RemoveMenuItem(repo: GcodeHistoryRepository, command: GcodeHistoryItem) : GcodeShortcutMenuItem(repo, command) {
        override val order = 5
        override val icon = R.drawable.ic_round_delete_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.remove_shortcut)
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            repo.removeEntry(command.command)
            host?.dismissAllowingStateLoss()
        }
    }
}