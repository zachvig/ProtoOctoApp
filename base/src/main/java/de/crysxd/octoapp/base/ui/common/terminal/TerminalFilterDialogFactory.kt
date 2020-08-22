package de.crysxd.octoapp.base.ui.common.terminal

import android.app.AlertDialog
import android.content.Context
import de.crysxd.octoapp.octoprint.models.settings.Settings


class TerminalFilterDialogFactory {

    fun showDialog(
        context: Context,
        filters: List<Pair<Settings.TerminalFilter, Boolean>>,
        onSelected: (List<Pair<Settings.TerminalFilter, Boolean>>) -> Unit
    ) {
        val updatedFilters = filters.toMutableList()

        AlertDialog.Builder(context)
            .setTitle("Select Filters")
            .setMultiChoiceItems(
                filters.map { it.first.name }.toTypedArray(),
                filters.map { it.second }.toBooleanArray()
            ) { _, which, isChecked ->
                updatedFilters[which] = Pair(updatedFilters[which].first, isChecked)
            }
            .setPositiveButton("Apply") { _, _ ->
                onSelected(updatedFilters)
            }
            .show()
    }
}