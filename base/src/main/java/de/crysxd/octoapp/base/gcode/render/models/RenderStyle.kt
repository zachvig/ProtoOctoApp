package de.crysxd.octoapp.base.gcode.render.models

import android.graphics.Paint
import android.graphics.drawable.Drawable
import de.crysxd.octoapp.base.gcode.parse.models.Move

data class RenderStyle(
    val paintPalette: (Move.Type) -> Paint,
    val background: Drawable?
)