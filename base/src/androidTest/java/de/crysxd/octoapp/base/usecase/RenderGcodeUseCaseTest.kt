package de.crysxd.octoapp.base.usecase

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.common.truth.Truth.assertThat
import de.crysxd.octoapp.base.gcode.CuraGcodeInterpreter
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

        measureTimeMillis {
            val renderParams = RenderGcodeUseCase.Params(
                gcode = gcode,
                directions = RenderGcodeUseCase.RenderDirections.ForLayerProgress(
                    layer = 1,
                    progress = 1f
                ), printBedSizeMm = PointF(235f, 235f),
                bitmap = bitmap,
                visibleRectMm = RectF(0f, 0f, 235f, 235f)
            )

            val bitmap = target.execute(renderParams)
            bitmap
        }.let {
            assertThat(it).isEqualTo(0)
        }
    }


    private val gcode = File("/data/data/de.crysxd.octoapp.base.test/cache/CE3_Green_box_engraved.gcode")
        .readText().let { CuraGcodeInterpreter().interpretFile(it) }
}
