package de.crysxd.octoapp.base.ui.menu.material

import android.content.Context
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ACTIVATE_MATERIAL
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.android.parcel.Parcelize

@Parcelize
class MaterialPluginMenu(val startPrintAfterSelection: FileObject.File? = null) : Menu {

    override fun getTitle(context: Context) = if (startPrintAfterSelection != null) "Select material to use" else "Materials"
    override fun getSubtitle(context: Context) = if (startPrintAfterSelection != null) {
        "The selected material will be activated for the print."
    } else {
        "You can edit these materials in the web interface."
    }

    override fun getBottomText(context: Context) = HtmlCompat.fromHtml(
        "<small>Materials are provided by the <a href=\"https://plugins.octoprint.org/plugins/SpoolManager/\">SpoolManager</a> or <a href=\"https://plugins.octoprint.org/plugins/filamentmanager/\">FilamentManager</a> plugin and can be edited in the web interface.</small>",
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    override fun getBottomMovementMethod(host: MenuBottomSheetFragment) = LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener())

    override suspend fun getMenuItem() = Injector.get().getMaterialsUseCase().execute(Unit).map {
        ActivateMaterialMenuItem(it.uniqueId, it.displayName, startPrintAfterSelection)
    }

    class ActivateMaterialMenuItem(
        private val uniqueMaterialId: String,
        private val displayName: String,
        private val startPrintAfterSelection: FileObject.File? = null
    ) : MenuItem {
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
        override suspend fun getTitle(context: Context) = "Activate „$displayName“"
        override suspend fun onClicked(host: MenuBottomSheetFragment, executeAsync: SuspendExecutor): Boolean {
            executeAsync {
                Injector.get().activateMaterialUseCase().execute(ActivateMaterialUseCase.Params(uniqueMaterialId))
                startPrintAfterSelection?.let {
                    Injector.get().startPrintJobUseCase().execute(StartPrintJobUseCase.Params(file = it, materialSelectionConfirmed = true))
                }
            }
            return true
        }
    }
}
