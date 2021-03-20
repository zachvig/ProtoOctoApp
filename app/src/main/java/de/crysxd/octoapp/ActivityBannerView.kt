package de.crysxd.octoapp

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.databinding.ActivityBannerViewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber

class ActivityBannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleRes: Int = 0) :
    LinearLayoutCompat(context, attrs, defStyleRes) {

    val binding = ActivityBannerViewBinding.inflate(LayoutInflater.from(context), this)
    var onStartShrink: () -> Unit = { }
    val shrinkJob: Job? = null
    private var runOnHide: () -> Unit = {}

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun show(activity: MainActivity, @StringRes message: Int, @DrawableRes icon: Int?, @ColorRes backgroundColor: Int, showSpinner: Boolean) {
        shrinkJob?.cancel()

        binding.icon.isVisible = true
        binding.text.isVisible = true
        if (icon == R.drawable.ic_octoeverywhere_24px) {
            binding.icon.clearColorFilter()
        } else {
            binding.icon.setColorFilter(binding.text.textColors.defaultColor)
        }
        binding.text.setText(message)
        binding.icon.setImageResource(icon ?: 0)
        binding.icon.isVisible = icon != null
        setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
        binding.progressBar.isVisible = showSpinner

        if (!showSpinner) {
            activity.lifecycleScope.launchWhenCreated {
                delay(5000)
                onStartShrink()
                binding.icon.isVisible = false
                binding.text.isVisible = false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = activity.window.decorView.systemUiVisibility
            val systemUiFlagsBackup = flags
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            activity.window.decorView.systemUiVisibility = flags
            runOnHide = {
                activity.window.decorView.systemUiVisibility = systemUiFlagsBackup
            }
        }
    }

    fun hide() {
        shrinkJob?.cancel()
        runOnHide
    }
}