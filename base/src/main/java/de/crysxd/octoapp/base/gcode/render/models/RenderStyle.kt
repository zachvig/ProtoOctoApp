package de.crysxd.octoapp.base.gcode.render.models

import android.graphics.Paint
import androidx.annotation.DrawableRes
import de.crysxd.octoapp.base.gcode.parse.models.Move

data class RenderStyle(
    val printHeadPaint: Paint,
    val paintPalette: (Move.Type) -> Paint,
    val previousLayerPaint: Paint,
    val remainingLayerPaint: Paint,
    @DrawableRes val background: Int
)