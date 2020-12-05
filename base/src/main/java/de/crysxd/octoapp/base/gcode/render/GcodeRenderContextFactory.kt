package de.crysxd.octoapp.base.gcode.render

import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodePath
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext

sealed class GcodeRenderContextFactory {

    protected val GcodePath.priority
        get() = when (type) {
            Move.Type.Travel -> 1
            Move.Type.Extrude -> 0
        }

    abstract fun extractMoves(gcode: Gcode): GcodeRenderContext

    data class ForFileLocation(val byte: Int) : GcodeRenderContextFactory() {
        override fun extractMoves(gcode: Gcode): GcodeRenderContext {
            val layer = gcode.layers.last { it.positionInFile <= byte }
            val paths = layer.moves.map {
                val count = it.value.first.count { i -> i.positionInFile <= byte }
                GcodePath(
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )
            }

            return GcodeRenderContext(paths.sortedBy { it.priority })
        }
    }

    data class ForLayerProgress(val layer: Int, val progress: Float) : GcodeRenderContextFactory() {
        override fun extractMoves(gcode: Gcode): GcodeRenderContext {
            val layer = gcode.layers[layer]
            val moveCount = layer.moveCount * progress
            val paths = layer.moves.map {
                val count = it.value.first.lastOrNull { i -> i.positionInLayer <= moveCount }?.let { i -> i.positionInArray + 4 } ?: 0
                GcodePath(
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )
            }

            return GcodeRenderContext(paths.sortedBy { it.priority })
        }
    }
}