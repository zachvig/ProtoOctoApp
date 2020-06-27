package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.android.synthetic.main.view_temperature.view.*

class TemperatureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_temperature, this)
        setTemperature(null)
    }

    fun setTemperature(temperature: PrinterState.ComponentTemperature?) {
        val actual = temperature?.actual?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        val target = temperature?.target?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        textViewTemperature.text = context.getString(R.string.temperature_x_of_y, actual, target)
    }

    fun setComponentName(name: String) {
        textViewComponentName.text = name
    }
}