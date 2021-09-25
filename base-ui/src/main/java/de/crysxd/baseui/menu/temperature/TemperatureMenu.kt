package de.crysxd.baseui.menu.temperature

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET
import de.crysxd.octoapp.base.usecase.SetTargetTemperaturesUseCase
import kotlinx.parcelize.Parcelize

@Parcelize
class TemperatureMenu : Menu {
    override fun shouldLoadBlocking() = true
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.temperature_menu___subtitle)
    override suspend fun getTitle(context: Context) = context.getString(R.string.temperature_menu___title)
    override suspend fun getMenuItem() = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
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
    override suspend fun onClicked(host: MenuHost?) {
        BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.settings?.temperature?.profiles?.firstOrNull {
            it.name == presetName
        }?.let {
            BaseInjector.get().setTargetTemperatureUseCase().execute(
                SetTargetTemperaturesUseCase.Params(
                    listOf(
                        SetTargetTemperaturesUseCase.Temperature(component = "tool0", temperature = it.extruder),
                        SetTargetTemperaturesUseCase.Temperature(component = "tool1", temperature = it.extruder),
                        SetTargetTemperaturesUseCase.Temperature(component = "tool2", temperature = it.extruder),
                        SetTargetTemperaturesUseCase.Temperature(component = "tool3", temperature = it.extruder),
                        SetTargetTemperaturesUseCase.Temperature(component = "bed", temperature = it.bed),
                        SetTargetTemperaturesUseCase.Temperature(component = "chamber", temperature = it.chamber),
                    )
                )
            )
        }
    }
}