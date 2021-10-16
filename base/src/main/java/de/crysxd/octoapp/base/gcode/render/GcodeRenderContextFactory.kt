package de.crysxd.octoapp.base.gcode.render

import android.graphics.PointF
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.LayerInfo
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodePath
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

sealed class GcodeRenderContextFactory {

    protected val ds = BaseInjector.get().localGcodeFileDataSource()
    private val GcodePath.priority
        get() = when (type) {
            Move.Type.Travel -> 2
            Move.Type.Extrude -> 1
            Move.Type.Unsupported -> 0
        }

    abstract suspend fun extractMoves(gcode: Gcode): GcodeRenderContext

    protected fun createContext(
        gcode: Gcode,
        layerIndex: Int,
        toPositionInFile: Int
    ): GcodeRenderContext {
        val layerInfo = gcode.layers[layerIndex]
        val completedCurrentLayer = loadSingleLayer(gcode.cacheKey, layerInfo, toPositionInFile = toPositionInFile)
        val remainingCurrentLayer = loadSingleLayer(gcode.cacheKey, layerInfo, fromPositionInFile = toPositionInFile)
        val previousLayer = gcode.layers.getOrNull(layerIndex - 1)?.let {
            loadSingleLayer(gcode.cacheKey, it)
        }

        val completedMoves = completedCurrentLayer.third.sumOf { it.moveCount }
        val allMoves = completedMoves + remainingCurrentLayer.third.sumOf { it.moveCount }

        return GcodeRenderContext(
            previousLayerPaths = previousLayer?.third,
            completedLayerPaths = completedCurrentLayer.third,
            remainingLayerPaths = remainingCurrentLayer.third,
            printHeadPosition = completedCurrentLayer.second,
            layerCount = gcode.layers.size,
            layerZHeight = layerInfo.zHeight,
            layerNumber = gcode.layers.indexOf(layerInfo),
            layerProgress = completedMoves / allMoves.toFloat()
        )
    }

    private fun loadSingleLayer(
        cacheKey: String,
        layerInfo: LayerInfo,
        fromPositionInFile: Int = 0,
        toPositionInFile: Int = Int.MAX_VALUE,
    ): Triple<Layer, PointF?, List<GcodePath>> {
        val layer = ds.loadLayer(cacheKey, layerInfo)
        var lastPosition: Pair<Int, PointF>? = null
        val paths = layer.moves.map {
            val moves = it.value.first

            // Find last move
            moves.firstOrNull { m ->
                m.positionInFile > toPositionInFile
            }?.let { m ->
                if (m.positionInFile > (lastPosition?.first ?: -1)) {
                    lastPosition = m.positionInFile to when (m) {
                        is Move.ArcMove -> m.endPosition
                        is Move.LinearMove -> {
                            val x = it.value.second[m.positionInArray + 2]
                            val y = it.value.second[m.positionInArray + 3]
                            PointF(x, y)
                        }
                    }
                }
            }

            // Find offset for lines array
            val linesOffset = if (fromPositionInFile == 0) {
                0
            } else {
                it.value.first.mapNotNull { m ->
                    m as? Move.LinearMove
                }.firstOrNull { i ->
                    i.positionInFile >= fromPositionInFile
                }?.positionInArray ?: 0
            }

            // Find count for lines array
            val linesCount = it.value.first.reversed().mapNotNull { m ->
                m as? Move.LinearMove
            }.firstOrNull { m ->
                m.positionInFile < toPositionInFile
            }?.let { m ->
                m.positionInArray + 4
            } ?: 0

            // Create path
            GcodePath(
                arcs = moves.mapNotNull { m -> (m as? Move.ArcMove) }.filter { m ->
                    m.positionInFile >= fromPositionInFile && m.positionInFile <= toPositionInFile
                },
                type = it.key,
                linesOffset = linesOffset,
                linesCount = linesCount - linesOffset,
                lines = it.value.second,
                moveCount = moves.size
            )
        }.sortedBy {
            it.priority
        }
        return Triple(layer, lastPosition?.second, paths)
    }

    data class ForFileLocation(val positionInFile: Int) : GcodeRenderContextFactory() {
        override suspend fun extractMoves(gcode: Gcode): GcodeRenderContext = withContext(Dispatchers.IO) {
            try {
                val layerIndex = gcode.layers.indexOfLast { it.positionInFile <= positionInFile }
                createContext(
                    gcode = gcode,
                    layerIndex = layerIndex,
                    toPositionInFile = positionInFile
                )
            } catch (e: Exception) {
                Timber.e(e)
                ds.removeFromCache(gcode.cacheKey)
                throw e
            }
        }
    }

    data class ForLayerProgress(val layerIndex: Int, val progress: Float) : GcodeRenderContextFactory() {
        override suspend fun extractMoves(gcode: Gcode): GcodeRenderContext = withContext(Dispatchers.IO) {
            try {
                val layerInfo = gcode.layers[layerIndex]
                val positionInFile = layerInfo.positionInFile + (layerInfo.lengthInFile * progress).roundToInt()
                createContext(
                    gcode = gcode,
                    layerIndex = layerIndex,
                    toPositionInFile = positionInFile
                ).copy(
                    printHeadPosition = null
                )
            } catch (e: Exception) {
                Timber.e(e)
                ds.removeFromCache(gcode.cacheKey)
                throw e
            }
        }
    }
}