package de.crysxd.octoapp

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.databinding.ActivityBannerViewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber

class ActivityBannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleRes: Int = 0) :
    LinearLayoutCompat(context, attrs, defStyleRes) {

    val binding = ActivityBannerViewBinding.inflate(LayoutInflater.from(context), this)
    var onStartShrink: () -> Unit = { }
    var shrinkJob: Job? = null
    private var runOnHide: () -> Unit = {}
    private var lastConfigHash = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
    }

    fun show(
        activity: MainActivity,
        @StringRes message: Int,
        @DrawableRes icon: Int?,
        @ColorRes backgroundColor: Int,
        showSpinner: Boolean,
        alreadyShrunken: Boolean,
        doOnShrink: () -> Unit = {},
    ) {
        val configHash = message + (icon ?: 0) + backgroundColor + showSpinner.hashCode()
        if (lastConfigHash == configHash) {
            return
        }
        lastConfigHash = configHash
        shrinkJob?.cancel()
        runOnHide()
        Timber.i("Showing: ${activity.getString(message)}")
        if (message == R.string.main___banner_connection_lost_reconnecting) {
            Timber.i("Sad")
        }

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
            shrinkJob = activity.lifecycleScope.launchWhenCreated {
                delay(5000)
                shrink()
                doOnShrink()
            }
        }

        if (alreadyShrunken) {
            shrink()
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

    private fun shrink() {
        onStartShrink()
        binding.icon.isVisible = false
        binding.text.isVisible = false
        shrinkJob?.cancel()
    }

    fun hide() {
        shrinkJob?.cancel()
        runOnHide()
        lastConfigHash = 0
    }
}