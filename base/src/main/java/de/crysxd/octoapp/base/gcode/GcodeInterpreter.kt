package de.crysxd.octoapp.base.gcode

import android.graphics.PointF
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class GcodeInterpreter {

    private var layers: MutableList<Layer> = mutableListOf()
    private var moves: MutableList<Move> = mutableListOf()
    private val lastPosition: PointF = PointF(0f, 0f)
    private var isAbsolutePositioningActive = true

    abstract fun canInterpretFile(content: String): Boolean

    fun interpretFile(content: String): Gcode {
        layers.clear()
        moves.clear()

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
            Move(
                from = PointF().also { it.x = lastPosition.x; it.y = lastPosition.y },
                to = PointF().also { it.x = absoluteX; it.y = absoluteY },
                type = type,
                positionInFile = positionInFile
            )
        )
    }

    abstract fun isLayerChange(line: String): Boolean

    private fun isMoveCommand(line: String) = line.startsWith("G1", true) || line.startsWith("G0", true)

    private fun isAbsolutePositioningCommand(line: String) = line.startsWith("G90", true)

    private fun isRelativePositioningCommand(line: String) = line.startsWith("G91", true)

    private fun startNewLayer() {
        layers.add(Layer(moves.toList()))
        moves.clear()
    }

    private fun addMove(move: Move) {
        moves.add(move)
        lastPosition.x = move.to.x
        lastPosition.y = move.to.y
    }

    fun Matcher.groupOrNull(index: Int) = if (matches() && groupCount() >= index) {
        group(index)
    } else {
        null
    }
}