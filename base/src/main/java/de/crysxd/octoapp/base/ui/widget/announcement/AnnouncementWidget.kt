package de.crysxd.octoapp.base.ui.widget.announcement

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.databinding.AnnouncementWidgetBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.common.TutorialView
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.octoprint.models.settings.Settings
import timber.log.Timber

class AnnouncementWidget(context: Context) : RecyclableOctoWidget<AnnouncementWidgetBinding, BaseViewModel>(context) {

    override val binding: AnnouncementWidgetBinding =
        AnnouncementWidgetBinding.inflate(LayoutInflater.from(context))

    init {
        Timber.i("Init")
        val hideAction = {
            if (!isVisible()) {
                parent.reloadWidgets()
            } else {
                parent.requestTransition()
            }
        }
        binding.whatsNew.onHideAction = hideAction
        binding.octoEverywhere.onHideAction = hideAction
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
        val repo = Injector.get().octorPrintRepository()
        val isAlreadyConnected = repo.getAll().any { it.alternativeWebUrl != null }
        val hasPlugin = repo.getActiveInstanceSnapshot()?.settings?.plugins?.values?.any { it is Settings.OctoEverywhere } == true
        val shouldAdvertise = Firebase.remoteConfig.getBoolean("advertise_octoeverywhere")
        val isVisible = TutorialView.isTutorialVisible(Injector.get().context().getString(R.string.pref_key_octoeverywhere_announcement))
        return !isAlreadyConnected && hasPlugin && isVisible && shouldAdvertise
    }

    private fun isWhatsNewVisible(): Boolean {
        return TutorialView.isTutorialVisible(Injector.get().context().getString(R.string.pref_key_version_announcement))
    }

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        isVisible()
    }

    override fun isVisible(): Boolean {
        // Settings visibility causes animation glitches?!
        binding.root.removeView(binding.whatsNew)
        binding.root.removeView(binding.octoEverywhere)

        if (isWhatsNewVisible()) {
            binding.root.addView(binding.whatsNew)
        }

        if (isOctoEverywhereAnnouncementVisible()) {
            binding.root.addView(binding.octoEverywhere)
        }

        val visible = isWhatsNewVisible() || isOctoEverywhereAnnouncementVisible()
        Timber.i("visible=$visible this=$this")
        return visible
    }

    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "announcement"
    override fun createNewViewModel(parent: BaseWidgetHostFragment): BaseViewModel? = null
}