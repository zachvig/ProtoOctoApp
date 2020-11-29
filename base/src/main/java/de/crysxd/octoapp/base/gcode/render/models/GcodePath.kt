package de.crysxd.octoapp.base.gcode.render.models

import de.crysxd.octoapp.base.gcode.parse.models.Move

class GcodePath(
    val points: FloatArray,
    val offset: Int,
    val count: Int,
    val type: Move.Type
)
