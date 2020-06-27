package de.crysxd.octoapp.base.ui.ext

import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.OctoActivity

fun Fragment.requireOctoActivity() : OctoActivity = requireActivity() as OctoActivity