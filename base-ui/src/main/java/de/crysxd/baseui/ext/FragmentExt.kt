package de.crysxd.baseui.ext

import androidx.fragment.app.Fragment
import de.crysxd.baseui.OctoActivity

fun Fragment.requireOctoActivity(): OctoActivity = requireActivity() as? OctoActivity
    ?: throw IllegalStateException("Fragment is not hosted by a OctoActivity")

fun Fragment.optionallyRequestOctoActivity(): OctoActivity? = activity as? OctoActivity
