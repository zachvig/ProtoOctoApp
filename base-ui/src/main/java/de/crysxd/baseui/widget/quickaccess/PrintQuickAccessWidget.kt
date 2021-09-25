package de.crysxd.baseui.widget.quickaccess

import android.content.Context
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.data.models.MenuId
import de.crysxd.octoapp.base.data.models.WidgetType

class PrintQuickAccessWidget(context: Context) : QuickAccessWidget(context) {
    override val menuId = MenuId.PrintWorkspace
    override val currentNavDestination = R.id.workspacePrint
    override val type = WidgetType.PrintQuickAccessWidget
}