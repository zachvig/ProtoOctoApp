package de.crysxd.octoapp.base.ext

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas


fun Drawable.toBitmapWithColor(context: Context, @ColorRes colorRes: Int) = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888).applyCanvas {
    val color = ContextCompat.getColor(context, colorRes)
    this@toBitmapWithColor.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    this@toBitmapWithColor.setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    this@toBitmapWithColor.draw(this)
}
