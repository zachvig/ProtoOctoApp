package de.crysxd.baseui.menu.material

import android.content.Context
import androidx.core.text.HtmlCompat
import de.crysxd.baseui.R
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.utils.NavigationResultMediator
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_ACTIVATE_MATERIAL
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.ActivateMaterialUseCase
import kotlinx.parcelize.Parcelize

@Parcelize
class MaterialPluginMenu(private val startPrintResultId: Int? = null) : Menu {

    override suspend fun getTitle(context: Context) =
        context.getString(if (startPrintResultId != null) R.string.material_menu___title_select_material else R.string.material_menu___title_neutral)

    override suspend fun getSubtitle(context: Context) =
        context.getString(if (startPrintResultId != null) R.string.material_menu___subtitle_select_material else R.string.material_menu___subtitle_neutral)

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
        LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.getMenuActivity()))

    override suspend fun getMenuItem() = listOf(
        BaseInjector.get().getMaterialsUseCase().execute(Unit).map {
            ActivateMaterialMenuItem(it.uniqueId, it.displayName, it.isActivated, startPrintResultId)
        },
        listOfNotNull(startPrintResultId?.let { PrintWithoutMaterialSelection(startPrintResultId) })
    ).flatten()

    class ActivateMaterialMenuItem(
        private val uniqueMaterialId: String,
        private val displayName: String,
        isActive: Boolean = false,
        private val startPrintResultId: Int? = null
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
        override fun getTitle(context: Context) =
            if (startPrintResultId != null) displayName else context.getString(R.string.material_menu___print_with_material, displayName)

        override suspend fun onClicked(host: MenuHost?) {
            BaseInjector.get().activateMaterialUseCase().execute(ActivateMaterialUseCase.Params(uniqueMaterialId))
            startPrintResultId?.let {
                NavigationResultMediator.postResult(startPrintResultId, true)
                host?.closeMenu()
            } ?: host?.reloadMenu()
        }
    }

    class PrintWithoutMaterialSelection(private val startPrintResultId: Int) : MenuItem {

        override val itemId = "noid"
        override var groupId = ""
        override val order = 322
        override val canBePinned = false
        override val style = MenuItemStyle.Printer
        override val icon = R.drawable.ic_round_layers_clear_24
        override fun getTitle(context: Context) = context.getString(R.string.material_menu___print_without_selection)
        override suspend fun onClicked(host: MenuHost?) {
            NavigationResultMediator.postResult(startPrintResultId, true)
            host?.closeMenu()
        }
    }
}
