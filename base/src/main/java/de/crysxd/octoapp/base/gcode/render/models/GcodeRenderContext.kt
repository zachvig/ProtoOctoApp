package de.crysxd.octoapp.base.gcode.render.models

import android.graphics.PointF

class GcodeRenderContext(
    val paths: List<GcodePath>,
    val printHeadPosition: PointF?,
    val layerCount: Int,
    val layerNumber: Int,
    val layerProgress: Float
)