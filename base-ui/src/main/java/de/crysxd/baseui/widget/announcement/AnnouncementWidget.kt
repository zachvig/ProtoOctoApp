package de.crysxd.baseui.widget.announcement

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.AnnouncementWidgetBinding
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.Announcement
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.purchaseOffers
import de.crysxd.octoapp.base.ext.toHtml
import timber.log.Timber

class AnnouncementWidget(context: Context) : RecyclableOctoWidget<AnnouncementWidgetBinding, BaseViewModel>(context) {
    override val type = WidgetType.AnnouncementWidget
    override val binding: AnnouncementWidgetBinding =
        AnnouncementWidgetBinding.inflate(LayoutInflater.from(context))

    init {
        Timber.i("Init")
        binding.announcement.hideAction = {
            recordInteraction()
            if (!isVisible()) {
                parent.reloadWidgets("hide-announcement")
            } else {
                parent.requestTransition()
            }
        }
    }

    private fun isSaleAnnouncementVisible() = Firebase.remoteConfig.purchaseOffers.activeConfig.advertisementWithData?.let {
        val banner = it.message.takeIf { b -> b.isNotBlank() } ?: return@let false
        binding.announcement.checkVisible(
            Announcement(
                text = { banner.toHtml() },
                actionText = { "Show" },
                backgroundColor = R.color.red_translucent,
                foregroundColor = R.color.red,
                id = it.id,
                actionUri = {
                    recordInteraction()
                    UriLibrary.getPurchaseUri()
                },
                refreshInterval = 0,
            )
        )
    } == true

    private fun isWhatsNewVisible() = BaseInjector.get().context().let {
        binding.announcement.checkVisible(
            Announcement(
                text = { getString(R.string.announcement___new_version_title) },
                actionText = { getString(R.string.announcement___new_version_learn_more) },
                id = context.getString(R.string.pref_key_version_announcement),
                actionUri = {
                    recordInteraction()
                    Uri.parse(getString(R.string.version_announcement_learn_more_link))
                },
                refreshInterval = 0,
            )
        )
    }

    private fun test() = BaseInjector.get().context().let {
        binding.announcement.checkVisible(
            Announcement(
                text = { "Test" },
                actionText = { null },
                id = "test",
                actionUri = {
                    recordInteraction()
                    Uri.parse(getString(R.string.version_announcement_learn_more_link))
                },
                refreshInterval = 0,
            )
        )
    }

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        isVisible()
    }

    override fun isVisible(): Boolean {
        // Check all announcements and show the most important one
        val isVisible = test() || isSaleAnnouncementVisible() || isWhatsNewVisible()

        Timber.i("visible=$isVisible this=$this")
        return isVisible
    }

    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "announcement"
    override fun createNewViewModel(parent: BaseWidgetHostFragment): BaseViewModel? = null
}