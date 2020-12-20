package de.crysxd.octoapp.base.ui.common.gcodeshortcut

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectParentViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.common.MenuBottomSheet
import java.lang.ref.WeakReference

class GcodeShortcutEditBottomSheet : MenuBottomSheet() {

    companion object {
        private const val ARG_COMMAND = "command"
        private var insertCallbackReference: WeakReference<(GcodeHistoryItem) -> Unit>? = null
        fun createForCommand(command: GcodeHistoryItem, insertCallback: ((GcodeHistoryItem) -> Unit)? = null) = GcodeShortcutEditBottomSheet().also {
            it.arguments = bundleOf(ARG_COMMAND to command)
            insertCallbackReference = WeakReference(insertCallback)
        }
    }

    // We need to use parent scope to prevent the set label action to die when the bottom sheet is closed
    private val viewModel: GcodeShortcutEditViewModel by injectParentViewModel(Injector.get().viewModelFactory())
    private val command get() = requireArguments().getParcelable<GcodeHistoryItem>(ARG_COMMAND)

    override fun getMenuRes() = R.menu.menu_gcode_edit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMenuItemVisibility(R.id.menuInsert, insertCallbackReference?.get() != null)
        viewModel.navContoller = findNavController()
        setTitle(command!!.oneLineCommand)
    }

    override suspend fun onMenuItemSelected(id: Int): Boolean {
        when (id) {
            R.id.menuClearLabel -> viewModel.clearLabel(command!!)
            R.id.menuSetLabel -> viewModel.updateLabel(requireContext(), command!!)
            R.id.menuRemove -> viewModel.remove(command!!)
            R.id.menuInsert -> insertCallbackReference?.get()?.invoke(command!!)
            R.id.menuToggleFavorite -> viewModel.setFavorite(command!!, !command!!.isFavorite)
            else -> return false
        }

        return true
    }
}