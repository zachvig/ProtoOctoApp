package de.crysxd.octoapp.base.utils

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import de.crysxd.octoapp.base.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme

class ThemePlugin(private val context: Context) : AbstractMarkwonPlugin() {

    override fun configureTheme(builder: MarkwonTheme.Builder) {
        super.configureTheme(builder)
        val res = context.resources
        val attrs = arrayOf(R.attr.fontFamily).toIntArray()
        val typeface = try {
            context.obtainStyledAttributes(
                R.style.OctoTheme_TextAppearance_Title,
                attrs
            ).use {
                ResourcesCompat.getFont(context, it.getResourceId(0, 0))
            }
        } catch (e: Exception) {
            null
        } ?: Typeface.DEFAULT

        builder.linkColor(ContextCompat.getColor(context, R.color.accent))
        builder.headingBreakHeight(0)
        builder.headingTextSizeMultipliers(
            arrayOf(
                1.714f, // H1
                1.571f, // H2
                1.429f, // H3
                1.286f, // H4
                1.143f, // H5
                1f, // H6
            ).toFloatArray()
        )
        builder.headingTypeface(typeface)
        builder.bulletWidth(res.getDimension(R.dimen.margin_0_1).toInt())
        builder.bulletListItemStrokeWidth(res.getDimension(R.dimen.margin_2).toInt())
    }
}