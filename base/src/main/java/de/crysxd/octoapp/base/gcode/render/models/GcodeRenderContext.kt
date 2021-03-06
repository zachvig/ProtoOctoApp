package de.crysxd.octoapp.base.gcode.render.models

import android.graphics.PointF

data class GcodeRenderContext(
    val paths: List<GcodePath>,
    val printHeadPosition: PointF?,
    val layerCount: Int,
    val layerNumber: Int,
    val layerZHeight: Float,
    val layerProgress: Float
)