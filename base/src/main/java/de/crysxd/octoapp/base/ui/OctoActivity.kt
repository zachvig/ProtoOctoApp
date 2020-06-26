package de.crysxd.octoapp.base.ui

import androidx.appcompat.app.AppCompatActivity
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView

abstract class OctoActivity : AppCompatActivity() {

    abstract val octoToolbar: OctoToolbar

    abstract val octo: OctoView

}