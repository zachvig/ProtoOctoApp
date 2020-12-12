package de.crysxd.octoapp.base.gcode.render.models

import android.graphics.PointF

class GcodeRenderContext(
    val paths: List<GcodePath>,
    val printHeadPosition: PointF?,
)