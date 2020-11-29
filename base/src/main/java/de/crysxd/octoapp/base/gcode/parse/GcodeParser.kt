package de.crysxd.octoapp.base.gcode.parse

import android.graphics.PointF
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.parse.models.Layer
import de.crysxd.octoapp.base.gcode.parse.models.Move
import timber.log.Timber
import java.io.InputStream
import java.util.regex.Matcher
import kotlin.math.roundToInt

val NEW_LAYER_MARKERS = arrayOf(
    ";LAYER:",
    ";AFTER_LAYER_CHANGE"
)

class GcodeParser {

    private var layers: MutableList<Layer> = mutableListOf()
    private var moves = mutableMapOf<Move.Type, Pair<MutableList<Move>, MutableList<Float>>>()
    private var moveCountInLayer = 0
    private val lastPosition: PointF = PointF(0f, 0f)
    private var isAbsolutePositioningActive = true

    suspend fun parseFile(content: InputStream, totalSize: Long, progressUpdate: suspend (Float) -> Unit): Gcode {
        layers.clear()
        initNewLayer()
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

    private fun parseLine(line: String, positionInFile: Int) {
        if (isComment(line)) {
            parseComment(line)
        } else {
            parseCommand(line, positionInFile)
        }
    }

    private fun isComment(line: String) = line.startsWith(";")

    private fun parseComment(line: String) = when {
        isLayerChange(line) -> startNewLayer()
        else -> Unit
    }

    private fun parseCommand(line: String, positionInFile: Int) = when {
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
        val x = extractValue("X", line) ?: return
        val y = extractValue("Y", line) ?: return
        val e = extractValue("E", line) ?: 0f

        // Convert to absolute position
        val absoluteX = if (isAbsolutePositioningActive) {
            x
        } else {
            lastPosition.x + x
        }
        val absoluteY = if (isAbsolutePositioningActive) {
            y
        } else {
            lastPosition.y + y
        }

        // Get type
        val type = if (e == 0f) {
            Move.Type.Travel
        } else {
            Move.Type.Extrude
        }

        addMove(
            type = type,
            positionInFile = positionInFile,
            fromX = lastPosition.x,
            fromY = lastPosition.y,
            toX = absoluteX,
            toY = absoluteY
        )
    }

    private fun isLayerChange(line: String) = NEW_LAYER_MARKERS.any { line.contains(it) }

    private fun isMoveCommand(line: String) = line.startsWith("G1", true) || line.startsWith("G0", true)

    private fun isAbsolutePositioningCommand(line: String) = line.startsWith("G90", true)

    private fun isRelativePositioningCommand(line: String) = line.startsWith("G91", true)

    private fun startNewLayer() {
        layers.add(
            Layer(
                moves = moves.mapValues {
                    Pair(it.value.first, it.value.second.toFloatArray())
                },
                moveCount = moves.map { it.value.first.size }.sum()
            )
        )
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
        lastPosition.x = toX
        lastPosition.y = toY
    }

    private fun Matcher.groupOrNull(index: Int) = if (groupCount() >= index) {
        group(index)
    } else {
        null
    }
}