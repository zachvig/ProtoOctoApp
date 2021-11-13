package de.crysxd.octoapp.printcontrols.ui.widget.progress

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.widget.TextViewCompat
import de.crysxd.octoapp.printcontrols.R
import kotlin.math.max
import kotlin.math.roundToInt

class ProgressItemView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int = 0) :
    ViewGroup(context, attributeSet, defStyleRes) {

    private val labelView = AppCompatTextView(context)
    private val valueView = AppCompatTextView(context)
    private val iconView = AppCompatImageView(context)

    @StringRes
    var label: Int = 0
        set(value) {
            field = value
            labelView.setText(value)
        }

    var labelIcon: Drawable?
        get() = iconView.drawable
        set(value) {
            iconView.setImageDrawable(value)
        }

    var value: CharSequence? = null
        set(value) {
            field = value
            valueView.text = value
        }

    var valueMaxLines: Int
        get() = valueView.maxLines
        set(value) {
            valueView.maxLines = value
        }
    var smallFont: Boolean = false
        set(value) {
            field = value
            setFont()
        }
    private var appliedSmallFont: Boolean? = null

    init {
        addView(labelView)
        addView(iconView)
        addView(valueView)
        setFont()
        labelView.setTextColor(ContextCompat.getColor(context, R.color.normal_text))
        valueView.setTextColor(ContextCompat.getColor(context, R.color.normal_text))
        valueView.ellipsize = TextUtils.TruncateAt.END

        context.obtainStyledAttributes(attributeSet, R.styleable.ProgressItemView).use {
            labelView.text = it.getString(R.styleable.ProgressItemView_label)
            valueView.text = it.getString(R.styleable.ProgressItemView_value)
            valueMaxLines = it.getInt(R.styleable.ProgressItemView_valueMaxLines, 1)
        }
    }

    private fun setFont() {
        if (smallFont != appliedSmallFont) {
            appliedSmallFont = smallFont
            if (smallFont) {
                TextViewCompat.setTextAppearance(labelView, R.style.OctoTheme_TextAppearance_Label_Small)
                TextViewCompat.setTextAppearance(valueView, R.style.OctoTheme_TextAppearance)
            } else {
                TextViewCompat.setTextAppearance(labelView, R.style.OctoTheme_TextAppearance_Label)
                TextViewCompat.setTextAppearance(valueView, R.style.OctoTheme_TextAppearance_Data)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)

        labelView.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )

        valueView.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val measuredWidth = max(labelView.measuredWidth, valueView.measuredWidth) + paddingLeft + paddingRight
        val measuredHeight = labelView.measuredHeight + valueView.measuredHeight + paddingTop + paddingBottom

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val iconSize = (labelView.measuredHeight * 0.8).roundToInt()
        val iconX = paddingLeft + labelView.measuredWidth + iconSize / 8
        val iconY = paddingTop + (labelView.measuredHeight - iconSize) / 2
        labelView.layout(paddingLeft, paddingTop, labelView.measuredWidth + paddingLeft, labelView.measuredHeight + paddingTop)
        iconView.layout(iconX, iconY, iconX + iconSize, iconY + iconSize)
        valueView.layout(paddingLeft, labelView.measuredHeight + paddingTop, r - paddingRight, b - paddingBottom)
    }
}