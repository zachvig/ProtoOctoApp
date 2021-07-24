package de.crysxd.octoapp.base.ui.widget.quickaccess

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.MenuId

class PrePrintQuickAccessWidget(context: Context) : QuickAccessWidget(context) {
    override val menuId = MenuId.PrePrintWorkspace
    override val currentNavDestination = R.id.workspacePrePrint
}