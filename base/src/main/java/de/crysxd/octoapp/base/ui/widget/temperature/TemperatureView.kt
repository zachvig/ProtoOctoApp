package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.get
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.android.synthetic.main.view_temperature.view.*
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

class TemperatureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val temperatureGradient = (ContextCompat.getDrawable(context, R.drawable.temp_grandient) as BitmapDrawable).bitmap
    private val temperatureDrawable = ContextCompat.getDrawable(context, R.drawable.temp_background)!!
    private val maxTemp: Int

    init {
        View.inflate(context, R.layout.view_temperature, this)
        setTemperature(null)

        val duration = 1000 + Math.random() * 1000
        backgroundImage.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_out_loop_2).also {
            it.duration = duration.toLong()
        })
        backgroundImageAnimated.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_out_loop).also {
            it.duration = duration.toLong()
        })

        maxTemp = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TemperatureView, 0, 0
        ).use {
            it.getInt(R.styleable.TemperatureView_maxTemp, 250)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val largerSize = sqrt(((right - left).toDouble().pow(2.0) + (bottom - top).toDouble().pow(2))).toInt()
        val rotated = Bitmap.createBitmap((right - left), (bottom - top), Bitmap.Config.ARGB_8888)
        rotated.applyCanvas {
            rotate((Math.random() * 360).toFloat(), width / 2f, height / 2f)
            val xOverhang = largerSize - width
            val yOverhang = largerSize - height
            temperatureDrawable.setBounds(-xOverhang, -yOverhang, width + xOverhang, height + yOverhang)
            temperatureDrawable.draw(this)
        }

        backgroundImageAnimated.setImageBitmap(rotated)

        backgroundImage.setImageBitmap(rotated)

    }

    fun setTemperature(temperature: PrinterState.ComponentTemperature?) {
        val actual = temperature?.actual?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        val target = temperature?.target?.toInt()?.toString()
        val offset = temperature?.offset?.toInt()?.toString()
        textViewTemperature.text = context.getString(R.string.temperature_x, actual)
        textViewTarget.text = when {
            target == null -> ""
            target == "0" -> context.getString(R.string.target_off)
            offset != null && offset != "0" -> context.getString(R.string.target_x_offset_y, target, offset)
            else -> context.getString(R.string.target_x, target)
        }
        backgroundImage.setColorFilter(getTemperatureColor(temperature?.actual), PorterDuff.Mode.SRC_IN)
        backgroundImageAnimated.setColorFilter(getTemperatureColor(temperature?.actual ?: 0 * 1.33f), PorterDuff.Mode.SRC_IN)
    }

    fun setComponentName(name: String) {
        textViewComponentName.text = name
    }

    private fun getTemperatureColor(temp: Float?): Int = try {
        val tempRange = 35..(maxTemp.coerceAtLeast(36))
        val cappedTemp = temp?.toInt()?.coerceIn(tempRange) ?: tempRange.first
        val tempPercent = ((cappedTemp - tempRange.first) / (tempRange.last - tempRange.first).toFloat())
        val y = (temperatureGradient.height - (tempPercent * temperatureGradient.height)).coerceAtMost(temperatureGradient.height - 1f)
        temperatureGradient[0, y.toInt()]
    } catch (e: Exception) {
        Timber.e(e)
        Color.GRAY
    }
}