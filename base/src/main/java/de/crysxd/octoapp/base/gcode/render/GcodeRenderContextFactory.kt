package de.crysxd.octoapp.base.gcode.render

import android.graphics.PointF
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.GcodePath
import de.crysxd.octoapp.base.gcode.render.models.GcodeRenderContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

sealed class GcodeRenderContextFactory {

    protected val GcodePath.priority
        get() = when (type) {
            Move.Type.Travel -> 2
            Move.Type.Extrude -> 1
            Move.Type.Unsupported -> 0
        }

    abstract suspend fun extractMoves(gcode: Gcode): GcodeRenderContext

    data class ForFileLocation(val byte: Int) : GcodeRenderContextFactory() {
        override suspend fun extractMoves(gcode: Gcode): GcodeRenderContext = withContext(Dispatchers.IO) {
            val ds = BaseInjector.get().localGcodeFileDataSource()
            try {
                val layerInfo = gcode.layers.last { it.positionInFile <= byte }
                val layer = ds.loadLayer(gcode.cacheKey, layerInfo)

                val paths = layer.moves.map {
                    val moves = it.value.first.takeWhile { i -> i.positionInFile <= byte }
                    val lastLinearMove = it.value.first.mapNotNull { it as? Move.LinearMove }.lastOrNull { i -> i.positionInFile <= byte }
                    val count = lastLinearMove?.let { i -> i.positionInArray + 4 } ?: 0
                    val path = GcodePath(
                        arcs = moves.mapNotNull { m -> (m as? Move.ArcMove) },
                        type = it.key,
                        offset = 0,
                        count = count,
                        points = it.value.second
                    )
                    Triple(it.key, moves, path)
                }

                val last = paths.map { it.first to it.second.lastOrNull() }
                    .filter { it.second != null }
                    .map { it.first to it.second!! }
                    .maxByOrNull { it.second.positionInFile }
                val lastMove = last?.second
                val lastType = last?.first

                val printHeadPosition = when (lastMove) {
                    is Move.ArcMove -> lastMove.endPosition
                    is Move.LinearMove -> {
                        layer.moves[lastType]?.let { moves ->
                            val x = moves.second[lastMove.positionInArray + 2]
                            val y = moves.second[lastMove.positionInArray + 3]
                            PointF(x, y)
                        }
                    }
                    null -> PointF(0f, 0f)
                }

                GcodeRenderContext(
                    printHeadPosition = printHeadPosition,
                    paths = paths.map { it.third }.sortedBy { it.priority },
                    layerCount = gcode.layers.size,
                    layerZHeight = layerInfo.zHeight,
                    layerNumber = gcode.layers.indexOf(layerInfo),
                    layerProgress = paths.sumOf { it.third.count } / layer.moves.values.sumOf { it.second.size }.toFloat()
                )
            } catch (e: Exception) {
                Timber.e(e)
                ds.removeFromCache(gcode.cacheKey)
                throw e
            }
        }
    }

    data class ForLayerProgress(val layerNo: Int, val progress: Float) : GcodeRenderContextFactory() {
        override suspend fun extractMoves(gcode: Gcode): GcodeRenderContext = withContext(Dispatchers.IO) {
            val ds = BaseInjector.get().localGcodeFileDataSource()
            try {
                val layerInfo = gcode.layers[layerNo]
                val layer = ds.loadLayer(gcode.cacheKey, layerInfo)
                val progressEnd = layerInfo.positionInFile + (layerInfo.lengthInFile * progress)
                val paths = layer.moves.map {
                    val moves = it.value.first.takeWhile { i -> i.positionInFile <= progressEnd }
                    val count = moves.mapNotNull { m -> m as? Move.LinearMove }.lastOrNull()?.let { m -> m.positionInArray + 4 } ?: 0
                    val path = GcodePath(
                        arcs = moves.mapNotNull { m -> (m as? Move.ArcMove) },
                        type = it.key,
                        offset = 0,
                        count = count,
                        points = it.value.second
                    )

                    path
                }

                GcodeRenderContext(
                    paths = paths.sortedBy { it.priority },
                    printHeadPosition = null,
                    layerNumber = layerNo,
                    layerCount = gcode.layers.size,
                    layerZHeight = layerInfo.zHeight,
                    layerProgress = progress
                )
            } catch (e: Exception) {
                Timber.e(e)
                ds.removeFromCache(gcode.cacheKey)
                throw e
            }
        }
    }
}