package de.crysxd.octoapp.base.ui.widget.quickaccess

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.MenuId

class PrintQuickAccessWidget(context: Context) : QuickAccessWidget(context) {
    override val menuId = MenuId.PrintWorkspace
    override val currentNavDestination = R.id.workspacePrint
}