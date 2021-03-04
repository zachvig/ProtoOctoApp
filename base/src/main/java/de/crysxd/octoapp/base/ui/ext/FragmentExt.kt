package de.crysxd.octoapp.base.ui.ext

import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.base.OctoActivity

fun Fragment.requireOctoActivity(): OctoActivity = OctoActivity.instance