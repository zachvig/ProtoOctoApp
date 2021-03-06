package de.crysxd.octoapp.base.ui.widget.announcement

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.AnnouncementWidgetBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.common.TutorialView
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import timber.log.Timber

class AnnouncementWidget(context: Context) : RecyclableOctoWidget<AnnouncementWidgetBinding, BaseViewModel>(context) {

    override val binding: AnnouncementWidgetBinding =
        AnnouncementWidgetBinding.inflate(LayoutInflater.from(context))

    init {
        Timber.i("Init")
        binding.root.onLearnMoreAction = {
            recordInteraction()
            parent.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(parent.getString(R.string.version_announcement_learn_more_link))))
        }
        binding.root.onHideAction = {
            parent.reloadWidgets()
        }
    }

    override fun isVisible() = TutorialView.isTutorialVisible(Injector.get().context().getString(R.string.pref_key_version_announcement))
    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "announcement"
    override fun createNewViewModel(parent: WidgetHostFragment): BaseViewModel? = null
}