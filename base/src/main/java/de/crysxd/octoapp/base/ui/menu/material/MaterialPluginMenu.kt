package de.crysxd.octoapp.base.ui.menu.material

import android.content.Context
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuHost
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ACTIVATE_MATERIAL
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import de.crysxd.octoapp.base.usecase.StartPrintJobUseCase
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.parcelize.Parcelize

@Parcelize
class MaterialPluginMenu(val startPrintAfterSelection: FileObject.File? = null) : Menu {

    override suspend fun getTitle(context: Context) =
        context.getString(if (startPrintAfterSelection != null) R.string.material_menu___title_select_material else R.string.material_menu___title_neutral)

    override suspend fun getSubtitle(context: Context) =
        context.getString(if (startPrintAfterSelection != null) R.string.material_menu___subtitle_select_material else R.string.material_menu___subtitle_neutral)

    override fun getEmptyStateSubtitle(context: Context) =
        context.getString(R.string.material_menu___empty_state)

    override fun getEmptyStateActionText(context: Context) = context.getString(R.string.material_menu___empty_state_action)
    override fun getEmptyStateActionUrl(context: Context) = UriLibrary.getFaqUri("supported_plugin").toString()
    override fun getEmptyStateIcon() = R.drawable.octo_materials

    override fun getBottomText(context: Context) = HtmlCompat.fromHtml(
        context.getString(R.string.material_menu___bottom_text),
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    override fun getBottomMovementMethod(host: MenuHost) =
        LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.getOctoActivity()))

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
        override suspend fun getTitle(context: Context) =
            if (startPrintAfterSelection != null) displayName else context.getString(R.string.material_menu___print_with_material, displayName)

        override suspend fun onClicked(host: MenuHost?) {
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
        override suspend fun getTitle(context: Context) = context.getString(R.string.material_menu___print_without_selection)
        override suspend fun onClicked(host: MenuHost?) {
            startPrintAfterSelection?.let {
                Injector.get().startPrintJobUseCase().execute(StartPrintJobUseCase.Params(file = it, materialSelectionConfirmed = true))
            }
        }
    }
}
