package de.crysxd.octoapp.base.gcode.parse.models

import java.io.Serializable

data class LayerInfo(
    val moveCount: Int,
    val zHeight: Float,
    val positionInFile: Int,
) : Serializable