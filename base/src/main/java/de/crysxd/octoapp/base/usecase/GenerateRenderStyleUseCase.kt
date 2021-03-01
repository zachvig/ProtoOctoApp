package de.crysxd.octoapp.base.usecase

import android.graphics.Color
import android.graphics.Paint
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import timber.log.Timber
import javax.inject.Inject

class GenerateRenderStyleUseCase @Inject constructor() : UseCase<OctoPrintInstanceInformationV2?, RenderStyle>() {

    companion object {
        private val extrudePaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = Color.WHITE
            strokeCap = Paint.Cap.BUTT
        }

        private val unsupportedPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = Color.RED
            strokeCap = Paint.Cap.BUTT
        }

        private val travelPaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            color = Color.GREEN
            strokeCap = Paint.Cap.BUTT
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
                    Move.Type.Unsupported -> unsupportedPaint
                }
            },
            background = R.drawable.print_bed_generic
        )
    }

    override suspend fun doExecute(param: OctoPrintInstanceInformationV2?, timber: Timber.Tree): RenderStyle = try {
        // Combine info to one string
        val info = param?.m115Response ?: ""

        // Set background based on machine type (somewhere in info)
        val backgroundRes = when {
            info.contains("charlotte", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("skr-mini-e3", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("skr_mini_e3", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("ender", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("creality", ignoreCase = true) -> R.drawable.print_bed_creality
            info.contains("cr-", ignoreCase = true) -> R.drawable.print_bed_creality
            info.contains("prusa", ignoreCase = true) -> R.drawable.print_bed_prusa
            info.contains("anycubic", ignoreCase = true) -> R.drawable.print_bed_anycubic
            info.contains("artillery", ignoreCase = true) -> R.drawable.print_bed_artillery
            info.contains("sidewinder", ignoreCase = true) -> R.drawable.print_bed_artillery
            else -> R.drawable.print_bed_generic
        }

        // Create style
        RenderStyle(
            printHeadPaint = printHeadPaint,
            paintPalette = {
                when (it) {
                    Move.Type.Travel -> travelPaint
                    Move.Type.Extrude -> extrudePaint
                    Move.Type.Unsupported -> unsupportedPaint
                }
            },
            background = backgroundRes
        )
    } catch (e: Exception) {
        Timber.e(e)
        defaultStyle
    }
}