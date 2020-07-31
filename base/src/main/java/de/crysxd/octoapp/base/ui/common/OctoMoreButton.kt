package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.base.R

class OctoMoreButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, style: Int = 0) : AppCompatImageButton(context, attrs, style) {

    init {
        setImageResource(R.drawable.ic_baseline_more_vert_24)
        setColorFilter(ContextCompat.getColor(context, R.color.button_more_foreground))
        setBackgroundResource(R.drawable.bg_button_more)
        outlineProvider = null
    }
}