package de.crysxd.baseui.widget.quickaccess

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.data.models.MenuId
import de.crysxd.octoapp.base.data.models.WidgetType

class PrePrintQuickAccessWidget(context: Context) : QuickAccessWidget(context) {
    override val menuId = MenuId.PrePrintWorkspace
    override val currentNavDestination = R.id.workspacePrePrint
    override val type = WidgetType.PrePrintQuickAccessWidget
}