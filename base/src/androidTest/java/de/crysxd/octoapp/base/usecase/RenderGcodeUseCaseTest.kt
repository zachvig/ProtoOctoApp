package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.common.truth.Truth.assertThat
import de.crysxd.octoapp.base.gcode.parse.CuraGcodeParser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

class RenderGcodeUseCaseTest {

    private val target = RenderGcodeUseCase()

    @Test
    fun testRender(): Unit = runBlocking {
        // Create bitmap
        val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        var paths: List<RenderGcodeUseCase.GcodePath> = emptyList()

        measureTimeMillis {
            val directions = RenderGcodeUseCase.RenderDirections.ForLayerProgress(
                layer = 1,
                progress = 1f
            )
            paths = directions.extractMoves(gcode)
        }.let {
            assertThat(it).isLessThan(10)
        }

        measureTimeMillis {
            val renderParams = RenderGcodeUseCase.Params(
                paths = paths,
                printBedSizeMm = PointF(235f, 235f),
                bitmap = bitmap,
                extrusionWidthMm = 0.5f,
                visibleRectMm = RectF(100f, 100f, 150f, 150f)
            )

            val bitmap = target.execute(renderParams)
            bitmap
        }.let {
            assertThat(it).isLessThan(16)
        }
    }


    private val gcode = File("/data/data/de.crysxd.octoapp.base.test/cache/CE3_Green_box_engraved.gcode")
        .readText().let { CuraGcodeParser().interpretFile(it) }
}
