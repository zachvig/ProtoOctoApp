package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.gcode.Gcode
import de.crysxd.octoapp.base.gcode.Move
import timber.log.Timber

class RenderGcodePreparationUseCase : UseCase<RenderGcodePreparationUseCase.Params, RenderGcodePreparationUseCase.GcodeRenderContext>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): GcodeRenderContext {
        val paths = param.directions.extractMoves(param.gcode).map {
            // Create a float array with the coordinates of all lines
            // This format is stupid, but super efficient as we can pass it directly to the GPU
            val points = FloatArray(it.value.size * 4)
            it.value.forEachIndexed { i, move ->
                points[i * 4] = move.from.x
                points[i * 4 + 1] = move.from.y
                points[i * 4 + 2] = move.to.x
                points[i * 4 + 3] = move.to.y
            }

            GcodePath(type = it.key, points = points)
        }

        return GcodeRenderContext(paths)
    }

    data class Params(
        val gcode: Gcode,
        val directions: RenderDirections
    )

    data class GcodeRenderContext(
        val paths: List<GcodePath>
    )

    class GcodePath(
        val points: FloatArray,
        val type: Move.Type
    )

    sealed class RenderDirections {
        abstract fun extractMoves(gcode: Gcode): Map<Move.Type, List<Move>>

        data class ForFileLocation(val byte: Int) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): Map<Move.Type, List<Move>> {
                TODO("Not yet implemented")
            }
        }

        data class ForLayerProgress(val layer: Int, val progress: Float) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): Map<Move.Type, List<Move>> {
                val layer = gcode.layers[layer]
                val moveCount = layer.moves.values.sumBy { it.size } * progress.coerceIn(0f, 1f)
                return layer.moves.map {
                    Pair(it.key, it.value.filter { it.positionInLayer <= moveCount })
                }.toMap()
            }
        }
    }
}