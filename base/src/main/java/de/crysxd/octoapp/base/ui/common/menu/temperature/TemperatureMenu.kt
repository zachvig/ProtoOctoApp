package de.crysxd.octoapp.base.ui.common.menu.temperature

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.common.menu.Menu
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.MenuItem
import de.crysxd.octoapp.base.ui.common.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.common.menu.main.MENU_ITEM_APPLY_TEMPERATURE_PRESET
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.SetBedTargetTemperatureUseCase
import de.crysxd.octoapp.base.usecase.SetToolTargetTemperatureUseCase
import kotlinx.android.parcel.Parcelize

@Parcelize
class TemperatureMenu : Menu {
    override fun getSubtitle(context: Context) = "You can change this presets in OctoPrint settings"
    override fun getTitle(context: Context) = "Temperature Presets"
    override fun getMenuItem() = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()
        ?.settings?.temperature?.profiles?.map {
            ApplyTemperaturePresetMenuItem(it.name)
        } ?: emptyList()
}

class ApplyTemperaturePresetMenuItem(val presetName: String) : MenuItem {
    companion object {
        fun forItemId(itemId: String) = ApplyTemperaturePresetMenuItem(itemId.replace(MENU_ITEM_APPLY_TEMPERATURE_PRESET, ""))
    }

    override val itemId = MENU_ITEM_APPLY_TEMPERATURE_PRESET + presetName
    override val order = 250
    override val style = MenuItemStyle.Printer
    override var groupId = ""
    override val icon = R.drawable.ic_round_local_fire_department_24
    override suspend fun getTitle(context: Context) = "Preheat $presetName"

    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.settings?.temperature?.profiles?.firstOrNull {
            it.name == presetName
        }?.let {
            Injector.get().setToolTargetTemperatureUseCase().execute(SetToolTargetTemperatureUseCase.Param(it.extruder ?: 0))
            Injector.get().setBedTargetTemperatureUseCase().execute(SetBedTargetTemperatureUseCase.Param(it.bed ?: 0))
            host.requireOctoActivity().showSnackbar(
                BaseViewModel.Message.SnackbarMessage(
                    text = { "$presetName applied" },
                    type = BaseViewModel.Message.SnackbarMessage.Type.Positive
                )
            )
        } ?: run {
            host.requireOctoActivity().showSnackbar(
                BaseViewModel.Message.SnackbarMessage(
                    text = { "$presetName not found, could not apply." },
                    type = BaseViewModel.Message.SnackbarMessage.Type.Negative
                )
            )
        }

        return true
    }
}