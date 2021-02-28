package de.crysxd.octoapp.base.ui.base

import android.graphics.Rect

interface InsetAwareScreen {
    fun handleInsets(insets: Rect)
}