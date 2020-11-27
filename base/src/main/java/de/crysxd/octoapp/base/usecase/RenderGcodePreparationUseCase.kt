package de.crysxd.octoapp.base.usecase

import android.graphics.PointF
import de.crysxd.octoapp.base.gcode.Gcode
import de.crysxd.octoapp.base.gcode.Move
import timber.log.Timber

class RenderGcodePreparationUseCase : UseCase<RenderGcodePreparationUseCase.Params, RenderGcodePreparationUseCase.GcodeRenderContext>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): GcodeRenderContext {
        val paths = mapOf(
            Move.Type.Extrude to Pair(android.graphics.Path(), PointF()),
            Move.Type.Travel to Pair(android.graphics.Path(), PointF())
        )

        param.directions.extractMoves(param.gcode).forEach {
            paths[it.type]?.let { (path, currentPosition) ->
                if (it.from != currentPosition) {
                    path.moveTo(it.from.x, it.from.y)
                }

                path.lineTo(it.to.x, it.to.y)
                currentPosition.x = it.to.x
                currentPosition.y = it.to.y
            }
        }

        return paths.map {
            GcodePath(type = it.key, path = it.value.first)
        }.let {
            GcodeRenderContext(it)
        }
    }

    data class Params(
        val gcode: Gcode,
        val directions: RenderDirections
    )

    data class GcodeRenderContext(
        val paths: List<GcodePath>
    )

    data class GcodePath(
        val path: android.graphics.Path,
        val type: Move.Type
    )

    sealed class RenderDirections {
        abstract fun extractMoves(gcode: Gcode): List<Move>

        data class ForFileLocation(val byte: Int) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): List<Move> {
                TODO("Not yet implemented")
            }

        }

        data class ForLayerProgress(val layer: Int, val progress: Float) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): List<Move> {
                val layer = gcode.layers[layer]
                val moveCount = layer.moves.size * progress.coerceIn(0f, 1f)
                return layer.moves.take(moveCount.toInt())
            }
        }
    }
}