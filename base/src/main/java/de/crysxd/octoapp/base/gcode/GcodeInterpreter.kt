package de.crysxd.octoapp.base.gcode

import android.graphics.PointF
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class GcodeInterpreter {

    private var layers: MutableList<Layer> = mutableListOf()
    private var moves = mutableMapOf<Move.Type, Pair<MutableList<Move>, MutableList<Float>>>()
    private val lastPosition: PointF = PointF(0f, 0f)
    private var isAbsolutePositioningActive = true

    abstract fun canInterpretFile(content: String): Boolean

    fun interpretFile(content: String): Gcode {
        layers.clear()
        initNewLayer()

        var positionInFile = 0
        content.split("\n").forEach {
            interpretLine(it, positionInFile)
            positionInFile += it.length
        }

        return Gcode(layers.toList())
    }

    private fun interpretLine(line: String, positionInFile: Int) {
        if (isComment(line)) {
            interpretComment(line)
        } else {
            interpretCommand(line, positionInFile)
        }
    }

    private fun isComment(line: String) = line.startsWith(";")

    private fun interpretComment(line: String) = when {
        isLayerChange(line) -> startNewLayer()
        else -> Unit
    }

    private fun interpretCommand(line: String, positionInFile: Int) = when {
        isAbsolutePositioningCommand(line) -> isAbsolutePositioningActive = true
        isRelativePositioningCommand(line) -> isAbsolutePositioningActive = false
        isMoveCommand(line) -> interpretMove(line, positionInFile)
        else -> Unit
    }

    private fun interpretMove(line: String, positionInFile: Int) {
        // Get positions
        val matcherX = Pattern.compile(".*X(\\d+\\.?\\d*).*").matcher(line)
        val matcherY = Pattern.compile(".*Y(\\d+\\.?\\d*).*").matcher(line)
        val matcherE = Pattern.compile(".*E(\\d+\\.?\\d*).*").matcher(line)
        matcherX.find()
        matcherY.find()
        matcherE.find()
        val x = matcherX.groupOrNull(1)?.toFloat() ?: return
        val y = matcherY.groupOrNull(1)?.toFloat() ?: return
        val e = matcherE.groupOrNull(1)?.toFloat() ?: 0f

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

    abstract fun isLayerChange(line: String): Boolean

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
        moves[Move.Type.Travel] = Pair(mutableListOf(), mutableListOf())
        moves[Move.Type.Extrude] = Pair(mutableListOf(), mutableListOf())
    }

    private fun addMove(type: Move.Type, positionInFile: Int, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        moves[type]?.let {
            val move = Move(
                positionInFile = positionInFile,
                positionInLayer = moves.values.sumBy { i -> i.first.size },
                positionInArray = it.second.size,
                type = type
            )

            it.first.add(move)
            it.second.add(fromX)
            it.second.add(fromY)
            it.second.add(toX)
            it.second.add(toY)
        }

        lastPosition.x = toX
        lastPosition.y = toY
    }

    private fun Matcher.groupOrNull(index: Int) = if (matches() && groupCount() >= index) {
        group(index)
    } else {
        null
    }
}