package de.crysxd.octoapp.base.gcode

data class Layer(
    val moves: Map<Move.Type, Pair<List<Move>, FloatArray>>,
    val moveCount: Int
)