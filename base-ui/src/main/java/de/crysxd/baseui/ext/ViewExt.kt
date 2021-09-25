package de.crysxd.baseui.ext

import android.view.View
import android.view.ViewGroup

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewGroup> View.findParent(): T? {
    var parent = parent as? ViewGroup
    while (parent != null && parent !is T) {
        parent = parent.parent as? ViewGroup
    }
    return parent as? T
}