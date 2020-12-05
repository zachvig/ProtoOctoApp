package de.crysxd.octoapp.pre_print_controls.ui

import android.graphics.Bitmap
import com.squareup.picasso.Transformation

class CropAlphaTransformation : Transformation {

    override fun key() = "crop_alpha"

    override fun transform(source: Bitmap): Bitmap {
        var minX: Int = source.width
        var minY: Int = source.height
        var maxX = -1
        var maxY = -1
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val alpha: Int = source.getPixel(x, y) shr 24 and 255
                if (alpha > 0) {
                    // pixel is not 100% transparent
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return if (maxX < minX || maxY < minY) {
            source
        } else {
            val result = Bitmap.createBitmap(
                source,
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
            )

            if (source != result) {
                source.recycle()
            }
            return result
        }
    }
}