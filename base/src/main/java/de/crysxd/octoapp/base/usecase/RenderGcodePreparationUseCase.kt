package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.gcode.Gcode
import de.crysxd.octoapp.base.gcode.Move
import timber.log.Timber

class RenderGcodePreparationUseCase : UseCase<RenderGcodePreparationUseCase.Params, RenderGcodePreparationUseCase.GcodeRenderContext>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): GcodeRenderContext {
        return GcodeRenderContext(param.directions.extractMoves(param.gcode))
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
        val offset: Int,
        val count: Int,
        val type: Move.Type
    )

    sealed class RenderDirections {
        abstract fun extractMoves(gcode: Gcode): List<GcodePath>

        data class ForFileLocation(val byte: Int) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): List<GcodePath> {
                TODO("Not yet implemented")
            }
        }

        data class ForLayerProgress(val layer: Int, val progress: Float) : RenderDirections() {
            override fun extractMoves(gcode: Gcode): List<GcodePath> {
                val layer = gcode.layers[layer]
                val moveCount = layer.moveCount * progress
                return layer.moves.map {
                    val count = it.value.first.last { i -> i.positionInLayer <= moveCount }.positionInArray
                    GcodePath(
                        type = it.key,
                        offset = 0,
                        count = count,
                        points = it.value.second
                    )
                }
            }
        }
    }
}