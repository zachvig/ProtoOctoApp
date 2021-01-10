package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog

class SettingsMenu : Menu {
    override fun getMenuItem(context: Context) = listOf(
        SendFeedbackMenuItem(context)
    )
}

class SendFeedbackMenuItem(context: Context) : MenuItem {
    override val itemId = "send_feedback"
    override val groupId = ""
    override val title = "Send Feedback"
    override val style = Style.Settings
    override val icon = R.drawable.ic_round_rate_review_24

    override fun onClicked(host: MenuBottomSheetFragment): Boolean {
        SendFeedbackDialog().show(host.parentFragmentManager, "feedback")
        return true
    }
}