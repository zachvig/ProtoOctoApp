package de.crysxd.octoapp.base.gcode.parse.models

import java.io.Serializable

data class Gcode(
    val layers: List<Layer>
) : Serializable