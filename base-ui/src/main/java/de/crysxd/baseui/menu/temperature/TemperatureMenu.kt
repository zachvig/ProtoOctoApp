package de.crysxd.baseui.menu.temperature

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_ALL
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_BED
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_CHAMBER
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_APPLY_TEMPERATURE_PRESET_HOTEND
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.BaseChangeTemperaturesUseCase
import de.crysxd.octoapp.octoprint.models.settings.Settings
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

@Parcelize
class TemperatureSubMenu(private val presetName: String) : Menu {
    override fun shouldLoadBlocking() = true
    override suspend fun getTitle(context: Context) = presetName
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.main_menu___submenu_subtitle)
    override suspend fun getMenuItem() = listOf(
        ApplyTemperaturePresetForAllMenuItem(presetName),
        ApplyTemperaturePresetForHotendMenuItem(presetName),
        ApplyTemperaturePresetForBedMenuItem(presetName),
        ApplyTemperaturePresetForChamberMenuItem(presetName)
    )
}

abstract class BaseApplyTemperaturePresetMenuItem(private val presetName: String) : MenuItem {
    override val style = MenuItemStyle.Printer
    override var groupId = ""
    override val icon = R.drawable.ic_round_local_fire_department_24
    override fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun onClicked(host: MenuHost?) {
        BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.settings?.temperature?.profiles?.firstOrNull {
            it.name == presetName
        }?.let {
            BaseInjector.get().setTargetTemperatureUseCase().execute(BaseChangeTemperaturesUseCase.Params(getTemperatures(it)))
        }
    }

    abstract suspend fun getTemperatures(profile: Settings.TemperatureProfile): List<BaseChangeTemperaturesUseCase.Temperature>
}

class ApplyTemperaturePresetMenuItem(private val presetName: String) : BaseApplyTemperaturePresetMenuItem(presetName) {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET + presetName
    override val order = 311
    override fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat, presetName)
    override val secondaryButtonIcon = R.drawable.ic_round_more_vert_24

    override suspend fun getTemperatures(profile: Settings.TemperatureProfile) = listOf(
        BaseChangeTemperaturesUseCase.Temperature(component = "tool0", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool1", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool2", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool3", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "bed", temperature = profile.bed),
        BaseChangeTemperaturesUseCase.Temperature(component = "chamber", temperature = profile.chamber),
    )

    override suspend fun onSecondaryClicked(host: MenuHost?) {
        host?.pushMenu(TemperatureSubMenu(presetName))
    }
}

class ApplyTemperaturePresetForAllMenuItem(private val presetName: String) : BaseApplyTemperaturePresetMenuItem(presetName) {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetForAllMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET_ALL, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET_ALL + presetName
    override val order = 312
    override fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat, presetName)

    override suspend fun getTemperatures(profile: Settings.TemperatureProfile) = listOf(
        BaseChangeTemperaturesUseCase.Temperature(component = "tool0", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool1", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool2", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool3", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "bed", temperature = profile.bed),
        BaseChangeTemperaturesUseCase.Temperature(component = "chamber", temperature = profile.chamber),
    )
}


class ApplyTemperaturePresetForHotendMenuItem(private val presetName: String) : BaseApplyTemperaturePresetMenuItem(presetName) {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetForHotendMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET_HOTEND, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET_HOTEND + presetName
    override val order = 312
    override fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat_hotend, presetName)

    override suspend fun getTemperatures(profile: Settings.TemperatureProfile) = listOf(
        BaseChangeTemperaturesUseCase.Temperature(component = "tool0", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool1", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool2", temperature = profile.extruder),
        BaseChangeTemperaturesUseCase.Temperature(component = "tool3", temperature = profile.extruder),
    )
}

class ApplyTemperaturePresetForBedMenuItem(private val presetName: String) : BaseApplyTemperaturePresetMenuItem(presetName) {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetForBedMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET_BED, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET_BED + presetName
    override val order = 313
    override fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat_bed, presetName)
    override fun isVisible(destinationId: Int) = super.isVisible(destinationId) &&
            BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.activeProfile?.heatedBed != false

    override suspend fun getTemperatures(profile: Settings.TemperatureProfile) = listOf(
        BaseChangeTemperaturesUseCase.Temperature(component = "bed", temperature = profile.bed),
    )
}

class ApplyTemperaturePresetForChamberMenuItem(private val presetName: String) : BaseApplyTemperaturePresetMenuItem(presetName) {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetForChamberMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET_CHAMBER, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET_CHAMBER + presetName
    override val order = 314
    override fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat_chamber, presetName)
    override fun isVisible(destinationId: Int) = super.isVisible(destinationId) &&
            BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.activeProfile?.heatedChamber != false

    override suspend fun getTemperatures(profile: Settings.TemperatureProfile) = listOf(
        BaseChangeTemperaturesUseCase.Temperature(component = "chamber", temperature = profile.chamber),
    )
}