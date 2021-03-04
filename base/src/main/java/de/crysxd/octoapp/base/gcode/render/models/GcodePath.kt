package de.crysxd.octoapp.base.gcode.render.models

import de.crysxd.octoapp.base.gcode.parse.models.Move

data class GcodePath(
    val arcs: List<Move.Arc>,
    val points: FloatArray,
    val offset: Int,
    val count: Int,
    val type: Move.Type
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GcodePath

        if (points.size != other.points.size) return false
        if (offset != other.offset) return false
        if (count != other.count) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = points.contentHashCode()
        result = 31 * result + offset
        result = 31 * result + count
        result = 31 * result + type.hashCode()
        return result
    }
}
