package de.crysxd.octoapp.base.ui.ext

import android.os.Build
import android.widget.TextView
import androidx.annotation.StyleRes
import com.google.android.material.resources.TextAppearance

fun TextView.setTextAppearanceCompat(@StyleRes textAppearance: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.setTextAppearance(textAppearance)
    } else {
        this.setTextAppearance(context, textAppearance)
    }
}