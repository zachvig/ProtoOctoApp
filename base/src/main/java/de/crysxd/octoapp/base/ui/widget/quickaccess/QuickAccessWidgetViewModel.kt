package de.crysxd.octoapp.base.ui.widget.quickaccess

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.repository.PinnedMenuItemRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class QuickAccessWidgetViewModel(
    private val pinnedMenuItemRepository: PinnedMenuItemRepository
) : BaseViewModel() {

    fun load(menuId: MenuId) = pinnedMenuItemRepository.observePinnedMenuItems(menuId).asLiveData()

    fun hasAny(menuId: MenuId) = pinnedMenuItemRepository.getPinnedMenuItems(menuId).isNotEmpty()

    fun toggle(menuId: MenuId, itemId: String) {
        pinnedMenuItemRepository.toggleMenuItemPinned(menuId, itemId)
    }

    fun execute(block: suspend () -> Unit) = viewModelScope.launch(coroutineExceptionHandler) { block() }
}