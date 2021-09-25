package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.graphics.Paint
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import timber.log.Timber
import javax.inject.Inject

class GenerateRenderStyleUseCase @Inject constructor(
    context: Context
) : UseCase<OctoPrintInstanceInformationV3?, RenderStyle>() {

    private val extrudePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.gcode_extrusion)
    }

    private val unsupportedPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.gcode_unsupported)
    }

    private val travelPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.gcode_travel)
    }

    private val printHeadPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.gcode_print_head)
    }

    private val defaultStyle = RenderStyle(
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

    override suspend fun doExecute(param: OctoPrintInstanceInformationV3?, timber: Timber.Tree): RenderStyle = try {
        // Combine info to one string
        val m115 = param?.m115Response ?: ""
        val profileName = param?.activeProfile?.name ?: ""
        val profileModel = param?.activeProfile?.model ?: ""
        val firmwareName = param?.systemInfo?.printerFirmware ?: ""

        // Info in priority order (first = highest)
        val info = listOf(firmwareName, profileName, profileModel, m115).joinToString()

        // Set background based on machine type (somewhere in info)
        val backgroundRes = when {
            info.contains("charlotte", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("skr-mini-e3", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("skr_mini_e3", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("ender", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("e3-", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("e3v2-", ignoreCase = true) -> R.drawable.print_bed_ender
            info.contains("e5-", ignoreCase = true) -> R.drawable.print_bed_ender
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