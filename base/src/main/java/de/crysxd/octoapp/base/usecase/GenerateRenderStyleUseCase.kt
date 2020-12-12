package de.crysxd.octoapp.base.usecase

import android.graphics.Color
import android.graphics.Paint
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import timber.log.Timber
import javax.inject.Inject

class GenerateRenderStyleUseCase @Inject constructor(
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase
) : UseCase<Unit, RenderStyle>() {

    companion object {
        private val extrudePaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
        }

        private val travelPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = Color.GREEN
            strokeCap = Paint.Cap.ROUND
        }

        private val printHeadPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = Color.RED
            strokeCap = Paint.Cap.ROUND
        }

        val defaultStyle = RenderStyle(
            printHeadPaint = printHeadPaint,
            paintPalette = {
                when (it) {
                    Move.Type.Travel -> travelPaint
                    Move.Type.Extrude -> extrudePaint
                }
            },
            background = R.drawable.print_bed_generic
        )
    }

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): RenderStyle = try {
        // Request firmware info from printer
        val response = executeGcodeCommandUseCase.execute(
            ExecuteGcodeCommandUseCase.Param(
                command = GcodeCommand.Single("M115"),
                fromUser = false,
                recordResponse = true
            )
        ).firstOrNull() as? ExecuteGcodeCommandUseCase.Response.RecordedResponse

        // Combine info to one string
        val info = response?.responseLines?.joinToString("") ?: ""

        // Set background based on machine type (somewhere in info)
        val backgroundRes = when {
            info.contains("charlotte", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("skr-mini-e3", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("ender", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("creality", ignoreCase = true) -> R.drawable.print_bed_creality
            info.contains("prusa", ignoreCase = true) -> R.drawable.print_bed_prusa
            else -> R.drawable.print_bed_generic
        }

        // Create style
        RenderStyle(
            printHeadPaint = printHeadPaint,
            paintPalette = {
                when (it) {
                    Move.Type.Travel -> travelPaint
                    Move.Type.Extrude -> extrudePaint
                }
            },
            background = backgroundRes
        )
    } catch (e: Exception) {
        Timber.e(e)
        defaultStyle
    }
}