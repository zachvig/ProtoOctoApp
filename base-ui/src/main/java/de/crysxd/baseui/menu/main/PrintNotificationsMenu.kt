package de.crysxd.baseui.menu.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.R
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.ToggleMenuItem
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_LIVE_NOTIFICATION
import de.crysxd.octoapp.base.data.models.MenuItems.MENU_ITEM_SYSTEM_NOTIFICATION_SETTINGS
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.toHtml
import kotlinx.parcelize.Parcelize
import timber.log.Timber


@Parcelize
class PrintNotificationsMenu : Menu {

    override suspend fun getMenuItem() = listOf(
        LiveNotificationMenuItem(),
        SystemNotificationSettings()
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.main_menu___item_print_notifications)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.print_notifications_menu___subtitle).toHtml()
    override fun getBottomText(context: Context) = context.getString(R.string.print_notifications_menu___bottom, UriLibrary.getFaqUri("print_notifications")).toHtml()
    override fun getBottomMovementMethod(host: MenuHost) = LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(host.getMenuActivity()))

    class LiveNotificationMenuItem : ToggleMenuItem() {
        override val isChecked get() = BaseInjector.get().octoPreferences().isLivePrintNotificationsEnabled
        override val itemId = MENU_ITEM_LIVE_NOTIFICATION
        override var groupId = ""
        override val order = 105
        override val enforceSingleLine = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_notifications_active_24
        override suspend fun getTitle(context: Context) = context.getString(R.string.print_notifications_menu___item_live_notification_on)
        override suspend fun getDescription(context: Context) = context.getString(R.string.print_notifications_menu___item_live_notification_on_description).toHtml()

        override suspend fun handleToggleFlipped(host: MenuHost, enabled: Boolean) {
            BaseInjector.get().octoPreferences().isLivePrintNotificationsEnabled = enabled

            try {
                if (enabled) {
                    Timber.i("Service enabled, starting service")
                    (host.getMenuActivity() as? OctoActivity)?.startPrintNotificationService()
                }
            } catch (e: IllegalStateException) {
                // User might have closed app just in time so we can't start the service
            }
        }
    }

    class SystemNotificationSettings : MenuItem {
        override val itemId = MENU_ITEM_SYSTEM_NOTIFICATION_SETTINGS
        override var groupId = ""
        override val order = 106
        override val enforceSingleLine = false
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_settings_24

        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
        override suspend fun isVisible(destinationId: Int) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        override suspend fun getTitle(context: Context) = context.getString(R.string.print_notifications_menu___item_system_settings)
        override suspend fun getDescription(context: Context) = context.getString(R.string.print_notifications_menu___item_system_settings_description).toHtml()

        override suspend fun onClicked(host: MenuHost?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                host?.getMenuActivity()?.let {
                    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, it.packageName)
                    it.startActivity(settingsIntent)
                }
            }
        }
    }
}