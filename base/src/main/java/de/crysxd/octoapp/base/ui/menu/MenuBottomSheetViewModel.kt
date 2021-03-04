package de.crysxd.octoapp.base.ui.menu

import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class MenuBottomSheetViewModel : BaseViewModel() {
    val menuBackStack = mutableListOf<Menu>()

    fun execute(block: suspend () -> Unit) = viewModelScope.launch(coroutineExceptionHandler) {
        block()
    }
}