package de.crysxd.octoapp.base.gcode.parse

import android.graphics.PointF
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.LayerInfo
import de.crysxd.octoapp.base.gcode.parse.models.Move
import timber.log.Timber
import java.io.InputStream
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class GcodeParser(
    private val content: InputStream,
    private val totalSize: Long,
    private val progressUpdate: suspend (Float) -> Unit,
    private val layerSink: suspend (Layer) -> Layer
) {

    private var layers: MutableList<LayerInfo> = mutableListOf()
    private var moves = mutableMapOf<Move.Type, Pair<MutableList<Move>, MutableList<Float>>>()
    private var moveCountInLayer = 0
    private var lastPosition: PointF? = null
    private var lastPositionZ = 0f
    private var lastExtrusionZ = 0f
    private var lastLayerChangeAtPositionInFile = 0
    private var isAbsolutePositioningActive = true

    suspend fun parseFile(): Gcode {
        layers.clear()
        initNewLayer()
        lastLayerChangeAtPositionInFile = 0
        lastPosition = null
        lastExtrusionZ = 0f
        lastExtrusionZ = 0f
        isAbsolutePositioningActive = true
        var positionInFile = 0
        var lastUpdatePercent = 0
        val reader = content.buffered()

        // This readLine function also returns \r and \n so we can properly keep track
        // of our position in the file. Ignoring \r will cause progress drift while printing.
        fun readLine(): Pair<String, Boolean> {
            val line = StringBuffer()
            while (true) {
                when (val read = reader.read()) {
                    -1 -> return line.toString() to false
                    '\n'.code -> return line.append('\n').toString() to true
                    else -> line.append(read.toChar())
                }
            }
        }

        while (true) {
            val (line, moreLines) = readLine()
            parseLine(line.takeWhile { it != ';' && it != '\r' && it != '\n' }, positionInFile)
            positionInFile += line.length

            val progress = (positionInFile / totalSize.toFloat())
            val percent = (progress * 100).roundToInt()
            if (lastUpdatePercent != percent) {
                progressUpdate(progress)
                lastUpdatePercent = percent
            }
            if (!moreLines) {
                break
            }
        }

        // Flush last layer
        startNewLayer(positionInFile)

        return Gcode(layers.toList(), "")
    }

    private suspend fun parseLine(line: String, positionInFile: Int) = when {
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

    private suspend fun parseLinearMove(line: String, positionInFile: Int) {
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
            positionInArray = 0,
        )
        addMove(
            move = move,
            type = type,
            fromX = lastPosition?.x ?: absoluteX,
            fromY = lastPosition?.y ?: absoluteY,
            toX = absoluteX,
            toY = absoluteY
        )
    }

    private suspend fun parseArcMove(line: String, positionInFile: Int) {
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
            r != null -> parseRFormArcMove(x = x, y = y, r = r, clockwise = clockwise, positionInFile = positionInFile)

            j != 0f || i != 0f -> parseIjFormArcMove(x = x, y = y, i = i, j = j, clockwise = clockwise, positionInFile = positionInFile)

            else -> throw IllegalArgumentException("Arc move without r or j or i value: $line")
        }

        addMove(
            move = move,
            fromX = lastPosition?.x ?: 0f,
            fromY = lastPosition?.y ?: 0f,
            toX = x ?: lastPosition?.x ?: 0f,
            toY = y ?: lastPosition?.y ?: 0f,
            type = type,
        )
    }

    private fun parseIjFormArcMove(x: Float?, y: Float?, i: Float, j: Float, clockwise: Boolean, positionInFile: Int): Move.ArcMove {
        // End positions are either the given X Y (always absolute) or if they are missing the last known ones
        val endX = x ?: lastPosition?.x ?: throw IllegalArgumentException("Missing param X")
        val endY = y ?: lastPosition?.y ?: throw IllegalArgumentException("Missing param Y")
        val startX = lastPosition?.x ?: throw java.lang.IllegalArgumentException("Missing start X")
        val startY = lastPosition?.y ?: throw java.lang.IllegalArgumentException("Missing start Y")
        val centerX = startX + i
        val centerY = startY + j
        val radius = sqrt(i.pow(2) + j.pow(2))

        fun Float.toDegrees() = (this * 180f / Math.PI).toFloat()
        fun getAndroidAngle(centerX: Float, centerY: Float, pointX: Float, pointY: Float): Float {
            // Control point marking 0deg
            val controlPointX = centerX + radius
            val controlPointY = centerY

            // Law of cosine
            val sideA = radius
            val sideB = radius
            val sideC = sqrt((pointX - controlPointX).pow(2) + (pointY - controlPointY).pow(2))
            val angle = acos((sideA.pow(2) + sideB.pow(2) - sideC.pow(2)) / (2 * sideA * sideB))
            val angleDegrees = angle.toDegrees()

            // If the angle in reality would exceed 180 deg, we need to flip the rectangle
            return if (pointY > centerY) {
                angleDegrees
            } else {
                360 - angleDegrees
            }
        }

        var startAngle = getAndroidAngle(centerX = centerX, centerY = centerY, pointX = startX, pointY = startY)
        val endAngle = getAndroidAngle(centerX = centerX, centerY = centerY, pointX = endX, pointY = endY)
        if (startAngle > endAngle) {
            startAngle -= 360
        }
        val sweepAngle = if (clockwise) {
            (endAngle - startAngle) - 360
        } else {
            endAngle - startAngle
        }

        return Move.ArcMove(
            leftX = centerX - radius,
            topY = centerY - radius,
            r = radius,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            positionInFile = positionInFile,
        )
    }

    private fun parseRFormArcMove(x: Float?, y: Float?, r: Float, clockwise: Boolean, positionInFile: Int): Move {
        return Move.LinearMove(
            positionInFile = positionInFile,
            positionInArray = 0,
        )
    }

    private suspend fun handleExtrusion(e: Float?, absoluteZ: Float, positionInFile: Int): Move.Type {
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

    private suspend fun startNewLayer(positionInFile: Int) {
        // Only add layer if we have any extrusion moves
        if (moves[Move.Type.Extrude]?.first?.isNotEmpty() == true) {
            val info = LayerInfo(
                zHeight = lastExtrusionZ,
                moveCount = moves.map { it.value.first.size }.sum(),
                positionInFile = lastLayerChangeAtPositionInFile,
                lengthInFile = positionInFile - lastLayerChangeAtPositionInFile
            )
            val layer = Layer(
                info = info,
                moves = moves.mapValues {
                    Pair(it.value.first, it.value.second.toFloatArray())
                }
            )
            val layerWithCacheInfo = layerSink(layer)
            layers.add(layerWithCacheInfo.info)
            lastLayerChangeAtPositionInFile = positionInFile
        }
        initNewLayer()
    }

    private fun initNewLayer() {
        moves.clear()
        moveCountInLayer = 0
        Move.Type.values().forEach {
            moves[it] = Pair(mutableListOf(), mutableListOf())
        }
    }

    private fun addMove(move: Move, type: Move.Type, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        when (move) {
            is Move.ArcMove -> moves[type]?.first?.add(move)
            is Move.LinearMove -> moves[type]?.let {
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