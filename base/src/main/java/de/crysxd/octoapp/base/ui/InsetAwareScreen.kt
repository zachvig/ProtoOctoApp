package de.crysxd.octoapp.base.ui

import android.graphics.Rect

interface InsetAwareScreen {
    fun handleInsets(insets: Rect)
}