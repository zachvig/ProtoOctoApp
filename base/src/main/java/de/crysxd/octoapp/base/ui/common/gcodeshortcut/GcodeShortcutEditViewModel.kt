package de.crysxd.octoapp.base.ui.common.gcodeshortcut

import android.content.Context
import android.text.InputType
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.repository.GcodeHistoryRepository
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragmentArgs
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GcodeShortcutEditViewModel(
    private val gcodeHistoryRepository: GcodeHistoryRepository
) : BaseViewModel() {

    fun clearLabel(command: GcodeHistoryItem) = viewModelScope.launch(coroutineExceptionHandler) {
        gcodeHistoryRepository.setLabelForGcode(command.command, null)
    }

    fun remove(command: GcodeHistoryItem) = viewModelScope.launch(coroutineExceptionHandler) {
        gcodeHistoryRepository.removeEntry(command.command)
    }

    fun updateLabel(context: Context, command: GcodeHistoryItem) = viewModelScope.launch(coroutineExceptionHandler) {
        val result = NavigationResultMediator.registerResultCallback<String?>()

        navContoller.navigate(
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
            gcodeHistoryRepository.setLabelForGcode(command = command.command, label = label)
        }
    }

    fun setFavorite(gcode: GcodeHistoryItem, favorite: Boolean) = viewModelScope.launch(coroutineExceptionHandler) {
        gcodeHistoryRepository.setFavorite(gcode.command, favorite)
    }
}