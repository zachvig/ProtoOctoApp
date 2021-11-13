package de.crysxd.baseui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import de.crysxd.baseui.databinding.AnnouncementViewBinding
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.base.data.models.Announcement
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open

class AnnouncementView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int = 0) : FrameLayout(
    context,
    attributeSet,
    defStyleRes
) {

    private var announcement: Announcement? = null
    private var binding: AnnouncementViewBinding? = null
    private var refreshTextRunnable: Runnable = Runnable { }
    var hideAction: () -> Unit = {}

    init {
        refreshTextRunnable = Runnable {
            binding?.text?.text = announcement?.text?.invoke(context)
            announcement?.refreshInterval?.takeIf { it > 0 }?.let {
                binding?.text?.postDelayed(refreshTextRunnable, it)
            }
        }
    }

    fun checkVisible(announcement: Announcement): Boolean {
        // Check if visible
        val prefs = if (isInEditMode) {
            return false
        } else {
            BaseInjector.get().sharedPreferences()
        }

        val prefsKey = "announcement_${announcement.id}_hidden"
        val isAnnouncementHidden = prefs.getBoolean(prefsKey, false)
        binding?.text?.removeCallbacks(refreshTextRunnable)

        // Bind
        if (!isAnnouncementHidden) {
            val actionText = announcement.actionText(context)
            this.announcement = announcement
            val b = binding ?: AnnouncementViewBinding.inflate(LayoutInflater.from(context), this, true)
            binding = b
            b.root.backgroundTintList = ContextCompat.getColorStateList(context, announcement.backgroundColor)
            b.buttonOpen.rippleColor = ContextCompat.getColorStateList(context, announcement.foregroundColor)
            b.buttonClose.rippleColor = ContextCompat.getColorStateList(context, announcement.foregroundColor)
            b.buttonOpen.setTextColor(ContextCompat.getColor(context, announcement.foregroundColor))
            b.buttonClose.setTextColor(ContextCompat.getColor(context, announcement.foregroundColor))

            b.buttonClose.setOnClickListener {
                prefs.edit { putBoolean(prefsKey, true) }
                hideAction()
            }
            b.buttonOpen.isVisible = !actionText.isNullOrBlank()
            b.buttonOpen.text = actionText
            b.buttonOpen.setOnClickListener {
                announcement.actionUri(it.context)?.open(findFragment<Fragment>().requireOctoActivity())
            }
            refreshTextRunnable.run()
        } else {
            this.announcement = null
        }

        return !isAnnouncementHidden
    }
}