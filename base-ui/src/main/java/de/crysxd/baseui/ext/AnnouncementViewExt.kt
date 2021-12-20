package de.crysxd.baseui.ext

import de.crysxd.baseui.R
import de.crysxd.baseui.common.AnnouncementView
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.Announcement

fun AnnouncementView.checkRemoteNotificationDisabledVisible(
    canHide: Boolean = true,
    id: String = context.getString(R.string.pref_key_play_services_error_announcement)
) = checkVisible(
    Announcement(
        text = { context.getString(R.string.announcement_play_services_error___text) },
        actionText = { context.getString(R.string.announcement_play_services_error___learn_more) },
        id = id,
        actionUri = {
            UriLibrary.getFaqUri("playServicesCrash")
        },
        canHide = canHide,
        refreshInterval = 0,
        backgroundColor = R.color.red_translucent,
        foregroundColor = R.color.red,
    )
)