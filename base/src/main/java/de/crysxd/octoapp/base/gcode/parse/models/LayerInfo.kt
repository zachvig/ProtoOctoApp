package de.crysxd.octoapp.base.gcode.parse.models

import java.io.Serializable

data class LayerInfo(
    val moveCount: Int,
    val zHeight: Float,
    val positionInFile: Int,
    val lengthInFile: Int,
    val positionInCacheFile: Long = 0,
    val lengthInCacheFile: Int = 0,
) : Serializable