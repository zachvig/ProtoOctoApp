package de.crysxd.octoapp.base.ui

import android.graphics.Color
import android.os.Parcelable
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import kotlinx.android.parcel.Parcelize
import timber.log.Timber


@Parcelize
class ColorTheme(@ColorRes val colorRes: Int) : Parcelable {

    val light = getWithAlpha(128)
    val dark = getWithAlpha(255)

    private fun getWithAlpha(alpha: Int): Int {
        val colorInt = ContextCompat.getColor(Injector.get().context(), colorRes)
        return Color.argb(
            alpha,
            Color.red(colorInt),
            Color.green(colorInt),
            Color.blue(colorInt),
        )
    }

    companion object {
        val default = ColorTheme(R.color.primary)
        val red = ColorTheme(R.color.red)
        val orange = ColorTheme(R.color.orange)
        val yellow = ColorTheme(R.color.yellow)
        val green = ColorTheme(R.color.green_2)
        val blue = ColorTheme(R.color.blue)
        val violet = ColorTheme(R.color.violet)

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
            Timber.i("Applying color theme for ${Injector.get().context().resources.getResourceEntryName(colorTheme.colorRes)}")
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

val OctoPrintInstanceInformationV2?.colorTheme
    get() = when (this?.settings?.appearance?.color) {
        "red" -> ColorTheme.red
        "orange" -> ColorTheme.orange
        "yellow" -> ColorTheme.yellow
        "green" -> ColorTheme.green
        "blue" -> ColorTheme.blue
        "violet" -> ColorTheme.violet
        else -> ColorTheme.default
    }