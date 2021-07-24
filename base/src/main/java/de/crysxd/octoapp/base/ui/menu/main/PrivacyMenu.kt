package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.menu.*
import kotlinx.parcelize.Parcelize

@Parcelize
class PrivacyMenu : Menu {

    override suspend fun getTitle(context: Context) = context.getString(R.string.privacy_menu___title)

    override suspend fun getSubtitle(context: Context) = context.getString(R.string.privacy_menu___subtitle).toHtml()

    override fun getBottomText(context: Context) =
        context.getString(R.string.privacy_menu___bottom_text).toHtml()


    override fun getBottomMovementMethod(host: MenuHost) =
        LinkClickMovementMethod(object : LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.getOctoActivity()) {
            override fun onLinkClicked(context: Context, url: String?): Boolean {
                return if (url == "mailto") {
                    host.getFragmentManager()?.let {
                        SendFeedbackDialog().show(it, "feedback")
                    }
                    true
                } else {
                    super.onLinkClicked(context, url)
                }
            }
        })

    override suspend fun getMenuItem(): List<MenuItem> = listOf(
        CrashReportingMenuItem(),
        AnalyticsMenuItem(),
    )

    class CrashReportingMenuItem : ToggleMenuItem() {
        override val isEnabled: Boolean get() = Injector.get().octoPreferences().isCrashReportingEnabled
        override val itemId = MENU_ITEM_CRASH_REPORTING
        override var groupId = ""
        override val order = 10000
        override val style = MenuItemStyle.Neutral
        override val icon = R.drawable.ic_round_bug_report_24
        override val canBePinned = false
        override suspend fun getTitle(context: Context) = context.getString(R.string.privacy_menu___crash_reporting_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.privacy_menu___crash_reporting_description)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().isCrashReportingEnabled = enabled
        }
    }

    class AnalyticsMenuItem : ToggleMenuItem() {
        override val isEnabled: Boolean get() = Injector.get().octoPreferences().isAnalyticsEnabled
        override val itemId = MENU_ITEM_ANALYTICS
        override var groupId = ""
        override val order = 10001
        override val style = MenuItemStyle.Neutral
        override val icon = R.drawable.ic_round_insert_chart_outlined_24
        override val canBePinned = false
        override suspend fun getTitle(context: Context) = context.getString(R.string.privacy_menu___analytics_title)
        override suspend fun getDescription(context: Context) = context.getString(R.string.privacy_menu___analytics_description)

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            Injector.get().octoPreferences().isAnalyticsEnabled = enabled
        }
    }
}