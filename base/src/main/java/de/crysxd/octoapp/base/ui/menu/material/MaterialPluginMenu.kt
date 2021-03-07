package de.crysxd.octoapp.base.ui.menu.material

import android.content.Context
import androidx.core.text.HtmlCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ACTIVATE_MATERIAL
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.parcelize.Parcelize

@Parcelize
class MaterialPluginMenu(val startPrintAfterSelection: FileObject.File? = null) : Menu {

    override suspend fun getTitle(context: Context) = if (startPrintAfterSelection != null) "Select material to use" else "Materials"
    override suspend fun getSubtitle(context: Context) = if (startPrintAfterSelection != null) {
        "The selected material will be activated for the print."
    } else {
        "You can edit these materials in the web interface."
    }

    override fun getEmptyStateSubtitle(context: Context) =
        "OctoApp integrates with the FilamentManager and the SpoolManager plugins to let you keep track of your materials. Once set up, you'll find your spools here."

    override fun getEmptyStateActionText(context: Context) = "Learn more"
    override fun getEmptyStateActionUrl(context: Context) = Firebase.remoteConfig.getString("help_url_materials")
    override fun getEmptyStateIcon() = R.drawable.octo_materials

    override fun getBottomText(context: Context) = HtmlCompat.fromHtml(
        "<small>„A“ means activated. Materials are provided by <a href=\"https://plugins.octoprint.org/plugins/SpoolManager/\">SpoolManager</a> or <a href=\"https://plugins.octoprint.org/plugins/filamentmanager/\">FilamentManager</a> and can be edited in the web interface.</small>",
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    override fun getBottomMovementMethod(host: MenuBottomSheetFragment) =
        LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.requireOctoActivity()))

    override suspend fun getMenuItem() = listOf(
        Injector.get().getMaterialsUseCase().execute(Unit).map {
            ActivateMaterialMenuItem(it.uniqueId, it.displayName, it.isActivated, startPrintAfterSelection)
        },
        listOf(PrintWithoutMaterialSelection(startPrintAfterSelection))
    ).flatten()

    class ActivateMaterialMenuItem(
        private val uniqueMaterialId: String,
        private val displayName: String,
        isActive: Boolean = false,
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
        override val icon = if (isActive) R.drawable.ic_round_layers_active_24 else R.drawable.ic_round_layers_24
        override suspend fun getTitle(context: Context) = if (startPrintAfterSelection != null) displayName else "Activate „$displayName“"
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            Injector.get().activateMaterialUseCase().execute(ActivateMaterialUseCase.Params(uniqueMaterialId))
            startPrintAfterSelection?.let {
                Injector.get().startPrintJobUseCase().execute(StartPrintJobUseCase.Params(file = it, materialSelectionConfirmed = true))
            }
        }
    }

    class PrintWithoutMaterialSelection(
        private val startPrintAfterSelection: FileObject.File? = null
    ) : MenuItem {

        override val itemId = "noid"
        override var groupId = ""
        override val order = 322
        override val canBePinned = false
        override suspend fun isVisible(destinationId: Int) = startPrintAfterSelection != null
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_layers_clear_24
        override suspend fun getTitle(context: Context) = "Print without selection"
        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            startPrintAfterSelection?.let {
                Injector.get().startPrintJobUseCase().execute(StartPrintJobUseCase.Params(file = it, materialSelectionConfirmed = true))
            }
        }
    }
}
