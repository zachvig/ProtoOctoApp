package de.crysxd.baseui.widget.announcement

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.BaseViewModel
import de.crysxd.baseui.R
import de.crysxd.baseui.common.TutorialView
import de.crysxd.baseui.databinding.AnnouncementWidgetBinding
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.Announcement
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.purchaseOffers
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.octoprint.models.settings.Settings
import timber.log.Timber

class AnnouncementWidget(context: Context) : RecyclableOctoWidget<AnnouncementWidgetBinding, BaseViewModel>(context) {
    override val type = WidgetType.AnnouncementWidget
    override val binding: AnnouncementWidgetBinding =
        AnnouncementWidgetBinding.inflate(LayoutInflater.from(context))

    init {
        Timber.i("Init")
        val hideAction = {
            if (!isVisible()) {
                parent.reloadWidgets("hide-announcement")
            } else {
                parent.requestTransition()
            }
        }
        binding.whatsNew.onHideAction = hideAction
        binding.octoEverywhere.onHideAction = hideAction
        binding.saleAnnouncement.hideAction = hideAction
        binding.whatsNew.onLearnMoreAction = {
            recordInteraction()
            Uri.parse(parent.getString(R.string.version_announcement_learn_more_link)).open(parent.requireOctoActivity())
        }
        binding.octoEverywhere.onLearnMoreAction = {
            recordInteraction()
            UriLibrary.getConfigureRemoteAccessUri().open(parent.requireOctoActivity())
        }
    }

    private fun isOctoEverywhereAnnouncementVisible(): Boolean {
        val repo = BaseInjector.get().octorPrintRepository()
        val isAlreadyConnected = repo.getAll().any { it.alternativeWebUrl != null }
        val hasPlugin = repo.getActiveInstanceSnapshot()?.settings?.plugins?.values?.any { it is Settings.OctoEverywhere } == true
        val shouldAdvertise = Firebase.remoteConfig.getBoolean("advertise_octoeverywhere")
        val isVisible = TutorialView.isTutorialVisible(BaseInjector.get().context().getString(R.string.pref_key_octoeverywhere_announcement))
        return !isAlreadyConnected && hasPlugin && isVisible && shouldAdvertise
    }

    private fun isSaleAnnouncementVisible() = Firebase.remoteConfig.purchaseOffers.activeConfig.advertisementWithData?.let {
        val banner = it.message.takeIf { b -> b.isNotBlank() } ?: return@let false
        binding.saleAnnouncement.checkVisible(
            Announcement(
                text = { banner.toHtml() },
                actionText = "Show",
                backgroundColor = R.color.red_translucent,
                foregroundColor = R.color.red,
                id = it.id,
                actionUri = UriLibrary.getPurchaseUri(),
                refreshInterval = 0,
            )
        )
    } == true

    private fun isWhatsNewVisible() = TutorialView.isTutorialVisible(BaseInjector.get().context().getString(R.string.pref_key_version_announcement))

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        isVisible()
    }

    override fun isVisible(): Boolean {
        // Settings visibility causes animation glitches?!
        binding.root.removeView(binding.whatsNew)
        binding.root.removeView(binding.octoEverywhere)
        var isVisible = false

        if (isWhatsNewVisible()) {
            binding.root.addView(binding.whatsNew)
            isVisible = true
        }

        if (isOctoEverywhereAnnouncementVisible()) {
            binding.root.addView(binding.octoEverywhere)
            isVisible = true
        }

        if (isSaleAnnouncementVisible()) {
            isVisible = true
        }

        Timber.i("visible=$isVisible this=$this")
        return isVisible
    }

    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "announcement"
    override fun createNewViewModel(parent: BaseWidgetHostFragment): BaseViewModel? = null
}