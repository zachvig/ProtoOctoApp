package de.crysxd.baseui.utils

import android.os.Parcelable
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.parcelize.Parcelize
import timber.log.Timber


@Parcelize
class ColorTheme(@ColorRes val colorRes: Int, @ColorRes val lightColorRes: Int, val order: Int) : Parcelable {

    val light get() = ContextCompat.getColor(BaseInjector.get().context(), lightColorRes)
    val dark get() = ContextCompat.getColor(BaseInjector.get().context(), colorRes)

    companion object {
        val white = ColorTheme(R.color.white_color_scheme, R.color.white_color_scheme_light, -4)
        val black = ColorTheme(R.color.black_color_scheme, R.color.black_color_scheme_light, -3)
        val blue = ColorTheme(R.color.blue_color_scheme, R.color.blue_color_scheme_light, -2)
        val violet = ColorTheme(R.color.violet_color_scheme, R.color.violet_color_scheme_light, -1)
        val red = ColorTheme(R.color.red_color_scheme, R.color.red_color_scheme_light, 0)
        val orange = ColorTheme(R.color.orange_color_scheme, R.color.orange_color_scheme_light, 1)
        val yellow = ColorTheme(R.color.yellow_color_scheme, R.color.yellow_color_scheme_light, 2)
        val default = ColorTheme(R.color.default_color_scheme, R.color.default_color_scheme_light, 3)
        val green = ColorTheme(R.color.green_color_scheme, R.color.green_color_scheme_light, 4)

        private val callbacks = mutableMapOf<View, (ColorTheme) -> Unit>()
        var activeColorTheme = default
            private set
        private val attachStateListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                callbacks.remove(v)
            }
        }

        fun applyColorTheme(colorTheme: ColorTheme) {
            Timber.i("Applying color theme for ${BaseInjector.get().context().resources.getResourceEntryName(colorTheme.colorRes)}")
            activeColorTheme = colorTheme
            callbacks.values.forEach { it(activeColorTheme) }
        }

        fun notifyAboutColorChangesUntilDetachedFromWindow(owner: View, cb: (ColorTheme) -> Unit) {
            cb(activeColorTheme)
            callbacks.remove(owner)
            callbacks[owner] = cb
            owner.addOnAttachStateChangeListener(attachStateListener)
        }
    }
}

val OctoPrintInstanceInformationV3?.colorTheme
    get() = when (this?.settings?.appearance?.color) {
        "red" -> ColorTheme.red
        "orange" -> ColorTheme.orange
        "yellow" -> ColorTheme.yellow
        "green" -> ColorTheme.green
        "blue" -> ColorTheme.blue
        "violet" -> ColorTheme.violet
        "white" -> ColorTheme.white
        "black" -> ColorTheme.black
        else -> ColorTheme.default
    }