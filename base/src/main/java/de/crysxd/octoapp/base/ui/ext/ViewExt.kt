package de.crysxd.octoapp.base.ui.ext

import android.view.View
import android.view.ViewGroup

@Suppress("UNCHECKED_CAST")
fun <T : ViewGroup> View.findParent(): T = when {
    parent == null -> throw IllegalStateException("Parent not found")
    parent as? T != null -> parent as T
    else -> (parent as ViewGroup).findParent()
}