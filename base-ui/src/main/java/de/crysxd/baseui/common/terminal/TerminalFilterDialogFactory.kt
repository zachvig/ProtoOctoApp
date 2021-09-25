package de.crysxd.baseui.common.terminal

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.octoprint.models.settings.Settings


class TerminalFilterDialogFactory {

    fun showDialog(
        context: Context,
        filters: List<Pair<Settings.TerminalFilter, Boolean>>,
        onSelected: (List<Pair<Settings.TerminalFilter, Boolean>>) -> Unit
    ) {
        val updatedFilters = filters.toMutableList()

        MaterialAlertDialogBuilder(context)
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