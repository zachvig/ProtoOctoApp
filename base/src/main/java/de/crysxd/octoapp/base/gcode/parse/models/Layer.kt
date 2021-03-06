package de.crysxd.octoapp.base.gcode.parse.models

import java.io.Serializable

data class Layer(
    val moves: Map<Move.Type, Pair<List<Move>, FloatArray>>,
    val info: LayerInfo
) : Serializable