package de.crysxd.octoapp.base.ui.ext

import android.view.View
import android.view.ViewGroup

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewGroup> View.findParent(): T {
    var parent = parent as? ViewGroup
    while (parent != null && parent !is T) {
        parent = parent.parent as? ViewGroup
    }
    parent ?: throw IllegalStateException("Unable to find a parent of type ${T::class.java.simpleName}")
    return parent as T
}