package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.gcode.parse.models.Move
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class GenerateRenderStyleUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val context: Context,
) : UseCase<Unit, RenderStyle>() {

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

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): RenderStyle {
        val firmwareData = octoPrintProvider.passiveFirmwareDataMessageFlow().first()
        val type = firmwareData.machineType ?: ""
        val backgroundRes = when {
            type.contains("charlotte", ignoreCase = true) -> R.drawable.print_bed_ender
            type.contains("skr-mini-e3", ignoreCase = true) -> R.drawable.print_bed_ender
            type.contains("ender", ignoreCase = true) -> R.drawable.print_bed_ender
            type.contains("creality", ignoreCase = true) -> R.drawable.print_bed_creality
            type.contains("prusa", ignoreCase = true) -> R.drawable.print_bed_prusa
            else -> R.drawable.print_bed_generic
        }

        return RenderStyle(
            paintPalette = {
                when (it) {
                    Move.Type.Travel -> travelPaint
                    Move.Type.Extrude -> extrudePaint
                }
            },
            background = ContextCompat.getDrawable(context, backgroundRes)
        )
    }
}