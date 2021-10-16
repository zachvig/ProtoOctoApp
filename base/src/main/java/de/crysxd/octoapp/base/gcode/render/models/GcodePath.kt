package de.crysxd.octoapp.base.gcode.render.models

import de.crysxd.octoapp.base.gcode.parse.models.Move

data class GcodePath(
    val arcs: List<Move.ArcMove>,
    val lines: FloatArray,
    val linesOffset: Int,
    val linesCount: Int,
    val type: Move.Type,
    val moveCount: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GcodePath

        if (lines.size != other.lines.size) return false
        if (linesOffset != other.linesOffset) return false
        if (linesCount != other.linesCount) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lines.contentHashCode()
        result = 31 * result + linesOffset
        result = 31 * result + linesCount
        result = 31 * result + type.hashCode()
        return result
    }
}
