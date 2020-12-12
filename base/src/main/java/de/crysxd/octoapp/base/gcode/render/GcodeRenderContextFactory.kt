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
                val move = it.value.first.lastOrNull { i -> i.positionInFile <= byte }
                val count = move?.let { i -> i.positionInArray + 4 } ?: 0
                val path = GcodePath(
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )
                Pair(move, path)
            }

            val printHeadPosition = paths.mapNotNull { it.first }.maxByOrNull { it.positionInFile }?.let {
                layer.moves[it.type]?.let { moves ->
                    val x = moves.second[it.positionInArray + 2]
                    val y = moves.second[it.positionInArray + 3]
                    PointF(x, y)
                }
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
                val count = it.value.first.lastOrNull { i -> i.positionInLayer <= moveCount }?.let { i -> i.positionInArray + 4 } ?: 0
                GcodePath(
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )
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