package de.crysxd.octoapp.base.gcode.parse

import android.graphics.PointF
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.Move
import timber.log.Timber
import java.io.InputStream
import kotlin.math.roundToInt

class GcodeParser {

    private var layers: MutableList<Layer> = mutableListOf()
    private var moves = mutableMapOf<Move.Type, Pair<MutableList<Move>, MutableList<Float>>>()
    private var moveCountInLayer = 0
    private var lastPosition: PointF? = null
    private var lastPositionZ = 0f
    private var lastExtrusionZ = 0f
    private var lastLayerChangeAtPositionInFile = 0
    private var isAbsolutePositioningActive = true

    suspend fun parseFile(content: InputStream, totalSize: Long, progressUpdate: suspend (Float) -> Unit): Gcode {
        layers.clear()
        initNewLayer()
        lastLayerChangeAtPositionInFile = 0
        var positionInFile = 0
        var lastUpdatePercent = 0

        content.reader().useLines { lines ->
            lines.iterator().forEach {
                parseLine(it, positionInFile)
                positionInFile += it.length

                val progress = (positionInFile / totalSize.toFloat())
                val percent = (progress * 100).roundToInt()
                if (lastUpdatePercent != percent) {
                    progressUpdate(progress)
                    lastUpdatePercent = percent
                }
            }
        }

        return Gcode(layers.toList())
    }

    private fun parseLine(line: String, positionInFile: Int) = when {
        isAbsolutePositioningCommand(line) -> isAbsolutePositioningActive = true
        isRelativePositioningCommand(line) -> isAbsolutePositioningActive = false
        isMoveCommand(line) -> interpretMove(line, positionInFile)
        else -> Unit
    }

    private fun extractValue(label: String, line: String): Float? {
        try {
            val start = line.indexOf(label)
            if (start < 0) return null
            val mappedStart = if (start < 0) return null else start + label.length
            val end = line.indexOf(' ', startIndex = mappedStart)
            val mappedEnd = if (end < 0) line.length else end
            return line.substring(mappedStart until mappedEnd).toFloat()
        } catch (e: NumberFormatException) {
            Timber.e(e, "Failed to extract `$label` from `$line`")
            throw e
        }
    }

    private fun interpretMove(line: String, positionInFile: Int) {
        // Get positions (don't use regex, it's slower)
        val x = extractValue("X", line)
        val y = extractValue("Y", line)
        val z = extractValue("Z", line) ?: lastPositionZ
        val e = extractValue("E", line) ?: 0f

        // Convert to absolute position
        // X and Y might be null. If so, we use the last known position as there was no movement
        val absoluteX = x?.let {
            if (isAbsolutePositioningActive) {
                it
            } else {
                (lastPosition?.x ?: 0f) + it
            }
        } ?: lastPosition?.x ?: 0f
        val absoluteY = y?.let {
            if (isAbsolutePositioningActive) {
                it
            } else {
                (lastPosition?.y ?: 0f) + it
            }
        } ?: lastPosition?.y ?: 0f
        val absoluteZ = if (isAbsolutePositioningActive) {
            z
        } else {
            lastExtrusionZ + z
        }

        // Get type
        val type = if (e == 0f) {
            Move.Type.Travel
        } else {
            Move.Type.Extrude
        }

        // Check if a new layer was started
        // A layer is started when we extrude (positive e, negative is retraction)
        // on a height which is different from the last height we extruded at
        if (e > 0) {
            // If the Z changed since the last extrusion, we have a new layer
            if (absoluteZ != lastExtrusionZ) {
                startNewLayer(positionInFile)
            }

            // Update last extrusion Z height
            lastExtrusionZ = absoluteZ
        }

        lastPositionZ = absoluteZ

        addMove(
            type = type,
            positionInFile = positionInFile,
            fromX = lastPosition?.x ?: absoluteX,
            fromY = lastPosition?.y ?: absoluteY,
            toX = absoluteX,
            toY = absoluteY
        )
    }

    private fun isMoveCommand(line: String) = line.startsWith("G1", true) || line.startsWith("G0", true)

    private fun isAbsolutePositioningCommand(line: String) = line.startsWith("G90", true)

    private fun isRelativePositioningCommand(line: String) = line.startsWith("G91", true)

    private fun startNewLayer(positionInFile: Int) {
        // Only add layer if we have any extrusion moves
        if (moves[Move.Type.Extrude]?.first?.isNotEmpty() == true) {
            layers.add(
                Layer(
                    zHeight = lastExtrusionZ,
                    moves = moves.mapValues {
                        Pair(it.value.first, it.value.second.toFloatArray())
                    },
                    moveCount = moves.map { it.value.first.size }.sum(),
                    positionInFile = lastLayerChangeAtPositionInFile
                )
            )

            lastLayerChangeAtPositionInFile = positionInFile
        }
        initNewLayer()
    }

    private fun initNewLayer() {
        moves.clear()
        moveCountInLayer = 0
        moves[Move.Type.Travel] = Pair(mutableListOf(), mutableListOf())
        moves[Move.Type.Extrude] = Pair(mutableListOf(), mutableListOf())
    }

    private fun addMove(type: Move.Type, positionInFile: Int, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        moves[type]?.let {
            val move = Move(
                positionInFile = positionInFile,
                positionInLayer = moveCountInLayer,
                positionInArray = it.second.size,
                type = type
            )

            it.first.add(move)
            it.second.add(fromX)
            it.second.add(fromY)
            it.second.add(toX)
            it.second.add(toY)
        }

        moveCountInLayer++

        if (lastPosition == null) {
            lastPosition = PointF(0f, 0f)
        }

        lastPosition?.x = toX
        lastPosition?.y = toY
    }
}