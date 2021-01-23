package de.crysxd.octoapp.base.ui.widget.announcement

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WidgetAnnouncementBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.TutorialView
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment

class AnnouncementWidget(parent: Fragment) : OctoWidget(parent) {

    private lateinit var binding: WidgetAnnouncementBinding

    override fun isVisible() = TutorialView.isTutorialVisible(Injector.get().context().getString(R.string.pref_key_version_announcement))
    override fun getTitle(context: Context) = null
    override fun getAnalyticsName() = "announcement"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup) = WidgetAnnouncementBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View) {
        binding.root.onLearnMoreAction = {
            recordInteraction()
            parent.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(parent.getString(R.string.version_announcement_learn_more_link))))
        }
        binding.root.onHideAction = {
            view.findFragment<WidgetHostFragment>().reloadWidgets()
        }
    }
}