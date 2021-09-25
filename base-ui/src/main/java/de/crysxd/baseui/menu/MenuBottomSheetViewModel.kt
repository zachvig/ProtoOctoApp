package de.crysxd.baseui.menu

import androidx.lifecycle.viewModelScope
import de.crysxd.baseui.BaseViewModel
import kotlinx.coroutines.launch

class MenuBottomSheetViewModel : BaseViewModel() {
    val menuBackStack = mutableListOf<Menu>()

    fun execute(block: suspend () -> Unit) = viewModelScope.launch(coroutineExceptionHandler) {
        block()
    }
}