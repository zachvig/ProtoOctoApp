package de.crysxd.octoapp.base.ui.common.menu

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.BaseViewModel
import kotlinx.coroutines.launch

class MenuBottomSheetViewModel : BaseViewModel() {
    val menuBackStack = mutableListOf<Menu>()

    fun execute(block: suspend () -> Unit) = viewModelScope.launch(coroutineExceptionHandler) {
        block()
    }
}