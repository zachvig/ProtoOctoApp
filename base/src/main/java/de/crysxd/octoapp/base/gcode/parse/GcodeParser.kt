package de.crysxd.octoapp.base.gcode.parse

import android.graphics.PointF
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.Move
import timber.log.Timber
import java.io.InputStream
import kotlin.math.*

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
        lastPosition = null
        lastExtrusionZ = 0f
        lastExtrusionZ = 0f
        isAbsolutePositioningActive = true
        var positionInFile = 0
        var lastUpdatePercent = 0

        content.reader().useLines { lines ->
            lines.iterator().forEach {
                parseLine(it.takeWhile { it != ';' }, positionInFile)
                positionInFile += it.length + 1

                val progress = (positionInFile / totalSize.toFloat())
                val percent = (progress * 100).roundToInt()
                if (lastUpdatePercent != percent) {
                    progressUpdate(progress)
                    lastUpdatePercent = percent
                }
            }
        }

        // Flush last layer
        startNewLayer(positionInFile)

        return Gcode(layers.toList())
    }

    private fun parseLine(line: String, positionInFile: Int) = when {
        isAbsolutePositioningCommand(line) -> isAbsolutePositioningActive = true
        isRelativePositioningCommand(line) -> isAbsolutePositioningActive = false
        isLinearMoveCommand(line) -> parseLinearMove(line, positionInFile)
        isArcMoveCommand(line) -> parseArcMove(line, positionInFile)
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
            return 0f
        }
    }

    private fun parseLinearMove(line: String, positionInFile: Int) {
        // Get positions (don't use regex, it's slower)
        val x = extractValue("X", line)
        val y = extractValue("Y", line)
        val z = extractValue("Z", line) ?: lastPositionZ
        val e = extractValue("E", line) ?: 0f

        // Convert to absolute position
        // X and Y might be null. If so, we use the last known position as there was no movement
        val (absoluteX, absoluteY, absoluteZ) = toAbsolutePosition(x = x, y = y, z = z)
        val type = handleExtrusion(e = e, absoluteZ = absoluteZ, positionInFile = positionInFile)

        val move = Move.LinearMove(
            positionInFile = positionInFile,
            positionInLayer = moveCountInLayer,
            positionInArray = 0,
            type = type
        )
        addMove(
            move = move,
            fromX = lastPosition?.x ?: absoluteX,
            fromY = lastPosition?.y ?: absoluteY,
            toX = absoluteX,
            toY = absoluteY
        )
    }

    private fun parseArcMove(line: String, positionInFile: Int) {
        // Get positions (don't use regex, it's slower)
        val x = extractValue("X", line)
        val y = extractValue("Y", line)
        val i = extractValue("I", line) ?: 0f
        val j = extractValue("J", line) ?: 0f
        val r = extractValue("R", line)
        val z = extractValue("Z", line) ?: lastPositionZ
        val e = extractValue("E", line) ?: 0f
        val clockwise = line.startsWith("G2")

        // Convert to absolute position
        // X and Y might be null. If so, we use the last known position as there was no movement
        val (_, _, absoluteZ) = toAbsolutePosition(x = 0f, y = 0f, z = z)
        val type = handleExtrusion(e = e, absoluteZ = absoluteZ, positionInFile = positionInFile)

        val move = when {
            r != null -> parseRFormArcMove(x = x, y = y, r = r, clockwise = clockwise, type = type, positionInFile = positionInFile)

            j != 0f || i != 0f -> parseIjFormArcMove(x = x, y = y, i = i, j = j, clockwise = clockwise, type = type, positionInFile = positionInFile)

            else -> throw IllegalArgumentException("Arc move without r or j or i value: $line")
        }

        addMove(
            move = move,
            fromX = lastPosition?.x ?: 0f,
            fromY = lastPosition?.y ?: 0f,
            toX = move.endX,
            toY = move.endY
        )
    }

    private fun parseIjFormArcMove(x: Float?, y: Float?, i: Float, j: Float, clockwise: Boolean, type: Move.Type, positionInFile: Int): Move.ArcMove {
        // End positions are either the given X Y (always absolute) or if they are missing the last known ones
        val endX = x ?: lastPosition?.x ?: throw IllegalArgumentException("Missing param X")
        val endY = y ?: lastPosition?.y ?: throw IllegalArgumentException("Missing param Y")
        val startX = lastPosition?.x ?: throw java.lang.IllegalArgumentException("Missing start X")
        val startY = lastPosition?.y ?: throw java.lang.IllegalArgumentException("Missing start Y")
        val centerX = startX + i
        val centerY = startY + j
        val r = sqrt(i.pow(2) + j.pow(2))

        // Vector from the center to the start point
        val centerToStartX = startX - centerX
        val centerToStartY = startY - centerY

        // Vector from the center to the end point
        val centerToEndX = endX - centerX
        val centerToEndY = endY - centerY

        // Vector from the center along what Android considers the 0deg axis
        val centerToControlX = 10f
        val centerToControlY = 0f

        // α = arccos[(xa * xb + ya * yb) / (√(xa^2 + ya^2) * √(xb^2 + yb^2))]
        fun Float.toDegrees() = (this * 180f / Math.PI).toFloat()
        fun getAndroidAngle(xa: Float, ya: Float, xb: Float = centerToControlX, yb: Float = centerToControlY) =
//            acos((xa * xb + ya * yb) / (sqrt(xa.pow(2) + ya.pow(2)) * sqrt(xb.pow(2) + yb.pow(2))))
            atan2(xb * ya - yb * xa, xb * xa + yb * ya)

        val angleToStart = getAndroidAngle(centerToStartX, centerToStartY).toDegrees()
        val angleToEnd = getAndroidAngle(centerToEndX, centerToEndY).toDegrees()
        val isFlipSide = angleToStart.absoluteValue == 180f
        val fixedAngleToEnd = if (isFlipSide) -angleToEnd else angleToEnd

        return Move.ArcMove(
            arc = Move.Arc(
                x0 = startX,
                y0 = startY,
                x1 = endX,
                y1 = endY,
                leftX = centerX - r,
                topY = centerY - r,
                r = r,
                startAngle = angleToStart,
                sweepAngle = if (isFlipSide) angleToStart - fixedAngleToEnd else fixedAngleToEnd - angleToStart,
            ),
            endX = endX,
            endY = endY,
            type = type,
            positionInLayer = moveCountInLayer,
            positionInFile = positionInFile,

            )
    }

    private fun parseRFormArcMove(x: Float?, y: Float?, r: Float, clockwise: Boolean, type: Move.Type, positionInFile: Int): Move.ArcMove {
        TODO()
    }

    private fun handleExtrusion(e: Float?, absoluteZ: Float, positionInFile: Int): Move.Type {
        // Check if a new layer was started
        // A layer is started when we extrude (positive e, negative is retraction)
        // on a height which is different from the last height we extruded at
        if (e == null || e > 0) {
            // If the Z changed since the last extrusion, we have a new layer
            if (absoluteZ != lastExtrusionZ) {
                startNewLayer(positionInFile)
            }

            // Update last extrusion Z height
            lastExtrusionZ = absoluteZ
        }

        lastPositionZ = absoluteZ

        // Get type
        return if (e == 0f) {
            Move.Type.Travel
        } else {
            Move.Type.Extrude
        }
    }

    private fun toAbsolutePosition(x: Float?, y: Float?, z: Float): Triple<Float, Float, Float> {
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

        return Triple(absoluteX, absoluteY, absoluteZ)
    }

    private fun isLinearMoveCommand(line: String) = isCommand(line = line, command = "G0") || isCommand(line = line, command = "G1")

    private fun isArcMoveCommand(line: String) = isCommand(line = line, command = "G2") || isCommand(line = line, command = "G3")

    private fun isAbsolutePositioningCommand(line: String) = isCommand(line = line, command = "G90")

    private fun isRelativePositioningCommand(line: String) = isCommand(line = line, command = "G91")

    private fun isCommand(line: String, command: String) =
        line.startsWith("$command ", ignoreCase = true) ||
                line.startsWith("$command;", ignoreCase = true) ||
                line.equals(command, ignoreCase = true)

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

    private fun addMove(move: Move, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        when (move) {
            is Move.ArcMove -> moves[move.type]?.first?.add(move)
            is Move.LinearMove -> moves[move.type]?.let {
                it.first.add(move.copy(positionInArray = it.second.size))
                it.second.add(fromX)
                it.second.add(fromY)
                it.second.add(toX)
                it.second.add(toY)
            }
        }

        moveCountInLayer++

        if (lastPosition == null) {
            lastPosition = PointF(0f, 0f)
        }

        lastPosition?.x = toX
        lastPosition?.y = toY
    }
}