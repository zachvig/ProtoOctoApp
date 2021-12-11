package de.crysxd.baseui.widget.temperature

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.get
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.ViewTemperatureBinding
import de.crysxd.octoapp.base.data.repository.TemperatureDataRepository
import timber.log.Timber

class TemperatureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val binding = ViewTemperatureBinding.inflate(LayoutInflater.from(context), this, true)
    private val temperatureGradient: Bitmap
    var maxTemp = 250
        set(value) {
            field = value
            binding.chart.viewport.setMinY(value.toDouble())
        }
    val button = binding.button
    private var currentBackground = Color.TRANSPARENT

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            temperatureGradient = Bitmap.createBitmap(10, 512, Bitmap.Config.ARGB_8888)
            val colors = arrayOf(
                ContextCompat.getColor(context, R.color.color_hot),
                ContextCompat.getColor(context, R.color.color_cold)
            ).toIntArray()
            val positions = arrayOf(
                0.3f,
                1f
            ).toFloatArray()
            val paint = Paint().also {
                it.style = Paint.Style.FILL
                it.shader = LinearGradient(0f, 0f, 0f, temperatureGradient.height.toFloat(), colors, positions, Shader.TileMode.CLAMP)
            }
            temperatureGradient.applyCanvas {
                drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        } else {
            temperatureGradient = (ContextCompat.getDrawable(context, R.drawable.temp_grandient) as BitmapDrawable).bitmap
        }

        setTemperature(null, null)
        binding.chart.alpha = 0.2f
        binding.chart.scaleX = 1.1f
        binding.chart.scaleY = 1.2f
        binding.chart.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
        binding.chart.gridLabelRenderer.isVerticalLabelsVisible = false
        binding.chart.gridLabelRenderer.isHorizontalLabelsVisible = false
        binding.chart.legendRenderer.isVisible = false
        binding.chart.viewport.isScrollable = false
        binding.chart.viewport.isScalable = false
    }

    fun setTemperature(temperature: TemperatureDataRepository.TemperatureSnapshot?, offset: Float?) {
        val actual = temperature?.current?.actual?.toInt()?.toString() ?: context.getString(R.string.no_value_placeholder)
        val target = temperature?.current?.target?.toInt()?.toString()
        val offsetInt = offset?.toInt()?.toString()
        binding.textViewTemperature.text = context.getString(R.string.temperature_x, actual)
        binding.textViewTarget.text = when {
            target == null -> ""
            target == "0" -> context.getString(R.string.target_off)
            offsetInt != null && offsetInt != "0" -> context.getString(R.string.target_x_offset_y, target, offsetInt)
            else -> context.getString(R.string.target_x, target)
        }

        ValueAnimator.ofArgb(currentBackground, getTemperatureColor(temperature?.current?.actual)).also {
            it.addUpdateListener {
                currentBackground = it.animatedValue as Int
                binding.root.setBackgroundColor(currentBackground)
            }
            it.interpolator = LinearInterpolator()
            it.duration = 500
        }.start()

        temperature?.history?.mapIndexed { index, it ->
            DataPoint(index.toDouble(), it.temperature.toDouble())
        }?.let {
            val series = LineGraphSeries(it.toTypedArray())
            series.backgroundColor = ContextCompat.getColor(context, R.color.inverse_text)
            series.color = Color.TRANSPARENT
            series.isDrawBackground = true
            binding.chart.removeAllSeries()
            binding.chart.addSeries(series)

            binding.chart.viewport.isXAxisBoundsManual = true
            binding.chart.viewport.setMinX(0.0)
            binding.chart.viewport.setMaxX(TemperatureDataRepository.MAX_ENTRIES.toDouble())
            binding.chart.viewport.isYAxisBoundsManual = true
            binding.chart.viewport.setMinY(0.0)
            binding.chart.viewport.setMaxY(maxTemp * 1.2)


        }
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
            100,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    } catch (e: Exception) {
        Timber.e(e)
        Color.GRAY
    }
}