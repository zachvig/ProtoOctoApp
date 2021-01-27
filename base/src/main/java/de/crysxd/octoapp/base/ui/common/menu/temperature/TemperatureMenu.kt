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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Parcelize
class TemperatureMenu : Menu {
    override fun getSubtitle(context: Context) = context.getString(R.string.temperature_menu___subtitle)
    override fun getTitle(context: Context) = context.getString(R.string.temperature_menu___title)
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
    override suspend fun getTitle(context: Context) = context.getString(R.string.temperature_menu___item_preheat, presetName)
    override suspend fun isVisible(destinationId: Int) = destinationId != R.id.workspaceConnect
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        GlobalScope.launch {
            val activity = host.requireOctoActivity()

            Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.settings?.temperature?.profiles?.firstOrNull {
                it.name == presetName
            }?.let {
                Injector.get().setToolTargetTemperatureUseCase().execute(SetToolTargetTemperatureUseCase.Param(it.extruder ?: 0))
                Injector.get().setBedTargetTemperatureUseCase().execute(SetBedTargetTemperatureUseCase.Param(it.bed ?: 0))
                activity.showSnackbar(
                    BaseViewModel.Message.SnackbarMessage(
                        text = { ctx -> ctx.getString(R.string.temperature_menu___applied_confirmation, presetName) },
                        type = BaseViewModel.Message.SnackbarMessage.Type.Positive
                    )
                )
            } ?: run {
                activity.showSnackbar(
                    BaseViewModel.Message.SnackbarMessage(
                        text = { ctx -> ctx.getString(R.string.temperature_menu___applied_error, presetName) },
                        type = BaseViewModel.Message.SnackbarMessage.Type.Negative
                    )
                )
            }
        }

        return true
    }
}