package de.crysxd.baseui.common.terminal

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.baseui.R
import de.crysxd.octoapp.octoprint.models.settings.Settings


class TerminalFilterDialogFactory {

    fun showDialog(
        context: Context,
        filters: List<Pair<Settings.TerminalFilter, Boolean>>,
        onSelected: (List<Pair<Settings.TerminalFilter, Boolean>>) -> Unit
    ) {
        val updatedFilters = filters.toMutableList()

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.terminal___select_filters))
            .setMultiChoiceItems(
                filters.map { it.first.name }.toTypedArray(),
                filters.map { it.second }.toBooleanArray()
            ) { _, which, isChecked ->
                updatedFilters[which] = Pair(updatedFilters[which].first, isChecked)
            }
            .setPositiveButton(context.getString(R.string.terminal___apply_filters)) { _, _ ->
                onSelected(updatedFilters)
            }
            .show()
    }
}