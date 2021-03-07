package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.get
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ViewTemperatureBinding
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import timber.log.Timber

class TemperatureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewTemperatureBinding.inflate(LayoutInflater.from(context), this, true)
    private val temperatureGradient = (ContextCompat.getDrawable(context, R.drawable.temp_grandient) as BitmapDrawable).bitmap
    private val maxTemp: Int
    val button = binding.button

    init {
        setTemperature(null)
        maxTemp = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TemperatureView, 0, 0
        ).use {
            it.getInt(R.styleable.TemperatureView_maxTemp, 250)
        }
    }

    fun setTemperature(temperature: PrinterState.ComponentTemperature?) {
        val actual = temperature?.actual?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        val target = temperature?.target?.toInt()?.toString()
        val offset = temperature?.offset?.toInt()?.toString()
        binding.textViewTemperature.text = context.getString(R.string.temperature_x, actual)
        binding.textViewTarget.text = when {
            target == null -> ""
            target == "0" -> context.getString(R.string.target_off)
            offset != null && offset != "0" -> context.getString(R.string.target_x_offset_y, target, offset)
            else -> context.getString(R.string.target_x, target)
        }
        binding.root.setBackgroundColor(getTemperatureColor(temperature?.actual))
    }

    fun setComponentName(name: String) {
        binding.textViewComponentName.text = name
    }

    private fun getTemperatureColor(temp: Float?): Int = try {
        val tempRange = 35..(maxTemp.coerceAtLeast(36))
        val cappedTemp = temp?.toInt()?.coerceIn(tempRange) ?: tempRange.first
        val tempPercent = ((cappedTemp - tempRange.first) / (tempRange.last - tempRange.first).toFloat())
        val y = (temperatureGradient.height - (tempPercent * temperatureGradient.height)).coerceAtMost(temperatureGradient.height - 1f)
        val color = temperatureGradient[0, y.toInt()]
        Color.argb(
            50,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    } catch (e: Exception) {
        Timber.e(e)
        Color.GRAY
    }
}