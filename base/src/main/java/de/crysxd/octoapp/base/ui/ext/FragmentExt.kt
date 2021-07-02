package de.crysxd.octoapp.base.ui.ext

import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.base.OctoActivity

fun Fragment.requireOctoActivity(): OctoActivity = requireActivity() as? OctoActivity
    ?: throw IllegalStateException("Fragment is not hosted by a OctoActivity")

fun Fragment.optionallyRequestOctoActivity(): OctoActivity? = requireActivity() as? OctoActivity
