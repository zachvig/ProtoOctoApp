package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.get
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.android.synthetic.main.view_temperature.view.*

class TemperatureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val temperatureGradient = ContextCompat.getDrawable(context, R.drawable.temp_grandient) as BitmapDrawable

    init {
        View.inflate(context, R.layout.view_temperature, this)
        setTemperature(null)
    }

    fun setTemperature(temperature: PrinterState.ComponentTemperature?) {
        val actual = temperature?.actual?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        val target = temperature?.target?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        textViewTemperature.text = context.getString(R.string.temperature_x_of_y, actual, target)
        backgroundImage.setColorFilter(getTemperatureColor(temperature?.actual), PorterDuff.Mode.SCREEN)
    }

    fun setComponentName(name: String) {
        textViewComponentName.text = name
    }

    private fun getTemperatureColor(temp: Float?): Int {
        val adjustedTemp = (temp ?: return Color.TRANSPARENT) - 25
        val y = ((adjustedTemp / 250f) * temperatureGradient.bitmap.height).toInt().coerceIn(0..250)
        return temperatureGradient.bitmap.get(0, temperatureGradient.bitmap.height - y - 1)
    }
}