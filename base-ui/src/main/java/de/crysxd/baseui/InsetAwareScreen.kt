package de.crysxd.baseui

import android.graphics.Rect

interface InsetAwareScreen {
    fun handleInsets(insets: Rect)
}