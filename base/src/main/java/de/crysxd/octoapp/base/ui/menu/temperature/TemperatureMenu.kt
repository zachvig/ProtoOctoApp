package de.crysxd.octoapp.base.ui.menu.temperature

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_APPLY_TEMPERATURE_PRESET
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import kotlinx.android.parcel.Parcelize

@Parcelize
class TemperatureMenu : Menu {
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.temperature_menu___subtitle)
    override suspend fun getTitle(context: Context) = context.getString(R.string.temperature_menu___title)
    override suspend fun getMenuItem() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()
        ?.settings?.temperature?.profiles?.map {
            ApplyTemperaturePresetMenuItem(it.name)
        } ?: emptyList()
}

class ApplyTemperaturePresetMenuItem(val presetName: String) : MenuItem {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET + presetName
    override val order = 311
    override val style = MenuItemStyle.Printer
    override var groupId = ""
    override val icon = R.drawable.ic_round_local_fire_department_24
    override suspend fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat, presetName)
    override suspend fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun onClicked(host: MenuBottomSheetFragment) {
        Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.settings?.temperature?.profiles?.firstOrNull {
            it.name == presetName
        }?.let {
            Injector.get().setToolTargetTemperatureUseCase().execute(SetToolTargetTemperatureUseCase.Param(it.extruder ?: 0))
            Injector.get().setBedTargetTemperatureUseCase().execute(SetBedTargetTemperatureUseCase.Param(it.bed ?: 0))
        }
    }
}