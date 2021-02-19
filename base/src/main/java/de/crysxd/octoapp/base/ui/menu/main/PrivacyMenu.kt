package de.crysxd.octoapp.base.ui.menu.main

import android.content.Context
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.menu.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class PrivacyMenu : Menu {

    override fun getTitle(context: Context) = "Privacy"

    override fun getSubtitle(context: Context) = HtmlCompat.fromHtml(
        "OctoApp does not collect any personalized information. I do not have any data about you" +
                " like your email address. Even if you are having a active subscription, you are completely anonymous.<br><br>OctoApp is a " +
                "<a href=\"https://gitlab.com/crysxd/octoapp\">open source</a> project.",
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    override fun getBottomText(context: Context) = HtmlCompat.fromHtml(
        "If you have any questions,<br>please reach out to <a href=\"mailto:hello@octoapp.eu\">hello@octoapp.eu</a><br><br><a href=\"https://octoapp-4e438.web.app/privacy\">Data privacy statement</a>",
        HtmlCompat.FROM_HTML_MODE_COMPACT
    )

    override fun getBottomMovementMethod(host: MenuBottomSheetFragment) = LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener())

    override fun getMenuItem(): List<MenuItem> = listOf(
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
        override suspend fun getTitle(context: Context) = "Anonymous crash reporting"
        override suspend fun getDescription(context: Context) =
            "If enabled, OctoApp sends automated crash reports to Firebase Crashlytics helping me to make the app stable for all users and devices. This reports are anonymous and deleted " +
                    "after 90 days. They contain generic information about your device (like model or Android version) and also the most recent logs collected by the " +
                    "app. Crash reports do not contain any sensitive data as all API keys, host names or Basic Auth credentials were scrubbed (best effort) " +
                    "while the error was created and again before the logs were cached."

        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
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
        override suspend fun getTitle(context: Context) = "Anonymous usage statistics"
        override suspend fun getDescription(context: Context) =
            "If enabled, OctoApp sends anonymous usage information to Firebase Analytics which will be deleted after 90 days. This information helps me to determine which features of the " +
                    "app are used the most and where I should spend time improving the app to benefit the most users. This information does not contain any data " +
                    "except which features of the app are used and basic information about your setup, e.g. which plugins are installed or which OctoPrint version " +
                    "is used. The data is not linked to any advertisement profile or something similar, if you reinstall the app or switch your device I don't have" +
                    " any means to connect your data."

        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            Injector.get().octoPreferences().isAnalyticsEnabled = enabled
        }
    }
}