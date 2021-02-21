package de.crysxd.octoapp.base.ui.menu.material

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ACTIVATE_MATERIAL
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import kotlinx.android.parcel.Parcelize

@Parcelize
class MaterialPluginMenu : Menu {

    override fun getTitle(context: Context) = "Materials"
    override fun getSubtitle(context: Context) =
        "You can edit these materials in the web interface. Materials are provided by the SpoolManager or FilamentManager plugin."

    override suspend fun getMenuItem() = Injector.get().getMaterialsUseCase().execute(Unit).map {
        ActivateMaterialMenuItem(it.uniqueId, it.displayName)
    }

    class ActivateMaterialMenuItem(private val uniqueMaterialId: String, private val displayName: String) : MenuItem {
        companion object {
            fun forItemId(itemId: String): ActivateMaterialMenuItem {
                val parts = itemId.split("/")
                return ActivateMaterialMenuItem(parts[1], parts[2])
            }
        }

        override val itemId = "$MENU_ITEM_ACTIVATE_MATERIAL/$uniqueMaterialId/$displayName"
        override var groupId = ""
        override val order = 321
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_layers_24
        override suspend fun getTitle(context: Context) = "Activate $displayName"
        override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
            executeAsync {
                Injector.get().activateMaterialUseCase().execute(ActivateMaterialUseCase.Params(uniqueMaterialId))
            }
            return true
        }
    }
}
