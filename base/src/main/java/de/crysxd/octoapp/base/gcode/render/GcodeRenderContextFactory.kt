package de.crysxd.octoapp.base.gcode.render

import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodePath
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import timber.log.Timber

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
            val layerNumber = gcode.layers.indexOf(layer)
            Timber.i("Layer #${layerNumber + 1}")
            Timber.i("Bytes printed: $byte")
            Timber.i("Layer start at byte: ${layer.positionInFile}")
            Timber.i("Layer end at byte: ${gcode.layers.getOrNull(layerNumber + 1)?.positionInFile}")
            val paths = layer.moves.map {
                val count = it.value.first.lastOrNull { i -> i.positionInFile <= byte }?.let { i -> i.positionInArray + 4 } ?: 0
                Timber.i("${it.key}: $count")
                GcodePath(
                    type = it.key,
                    offset = 0,
                    count = count,
                    points = it.value.second
                )
            }
            Timber.i("======")
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