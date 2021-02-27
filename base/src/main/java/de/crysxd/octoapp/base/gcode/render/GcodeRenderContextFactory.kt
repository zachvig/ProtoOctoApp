package de.crysxd.octoapp.base.gcode.render

import android.graphics.PointF
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
                val moves = it.value.first.takeWhile { i -> i.positionInFile <= byte }
                val lastLinearMove = it.value.first.mapNotNull { it as? Move.LinearMove }.lastOrNull { i -> i.positionInFile <= byte }
                val count = lastLinearMove?.let { i -> i.positionInArray + 4 } ?: 0
                val path = GcodePath(
                    arcs = moves.mapNotNull { m -> (m as? Move.ArcMove)?.arc },
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )
                moves to path
            }

            val printHeadPosition = when (val lastMove = paths.map { it.first.last() }.maxByOrNull { it.positionInFile }) {
                is Move.ArcMove -> PointF(lastMove.endX, lastMove.endY)
                is Move.LinearMove -> {
                    layer.moves[lastMove.type]?.let { moves ->
                        val x = moves.second[lastMove.positionInArray + 2]
                        val y = moves.second[lastMove.positionInArray + 3]
                        PointF(x, y)
                    }
                }
                null -> PointF(0f, 0f)
            }

            return GcodeRenderContext(
                printHeadPosition = printHeadPosition,
                paths = paths.map { it.second }.sortedBy { it.priority },
                layerCount = gcode.layers.size,
                layerZHeight = layer.zHeight,
                layerNumber = gcode.layers.indexOf(layer),
                layerProgress = paths.sumBy { it.second.count } / layer.moves.values.sumBy { it.second.size }.toFloat()
            )
        }
    }

    data class ForLayerProgress(val layer: Int, val progress: Float) : GcodeRenderContextFactory() {
        override fun extractMoves(gcode: Gcode): GcodeRenderContext {
            val layer = gcode.layers[layer]
            val moveCount = layer.moveCount * progress
            val paths = layer.moves.map {
                val moves = it.value.first.takeWhile { i -> i.positionInLayer <= moveCount }
                val count = moves.mapNotNull { m -> m as? Move.LinearMove }.lastOrNull()?.let { m -> m.positionInArray + 4 } ?: 0
                val path = GcodePath(
                    arcs = moves.mapNotNull { m -> (m as? Move.ArcMove)?.arc },
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )

                path
            }

            return GcodeRenderContext(
                paths = paths.sortedBy { it.priority },
                printHeadPosition = null,
                layerNumber = this.layer + 1,
                layerCount = gcode.layers.size,
                layerZHeight = layer.zHeight,
                layerProgress = progress
            )
        }
    }
}