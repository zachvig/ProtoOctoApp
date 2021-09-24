package de.crysxd.octoapp.widgets.quickaccess

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.PreparedMenuItem
import de.crysxd.octoapp.base.ui.menu.main.MenuItemLibrary
import de.crysxd.octoapp.widgets.ExecuteWidgetActionActivity
import kotlinx.coroutines.runBlocking

class QuickAccessRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {

    private val repository = Injector.get().pinnedMenuItemsRepository()
    private val library = MenuItemLibrary()
    private val items
        get() = repository.getPinnedMenuItems(MenuId.Widget).toList().mapNotNull {
            library[it]
        }.sortedBy {
            it.order
        }

    override fun onCreate() = Unit
    override fun onDataSetChanged() = Unit
    override fun onDestroy() = Unit
    override fun getCount() = items.size
    override fun getLoadingView() = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = items[position].hashCode().toLong()
    override fun hasStableIds() = true

    override fun getViewAt(position: Int): RemoteViews? {
        val item = items.getOrNull(position) ?: return null
        val preparedItem = runBlocking {
            PreparedMenuItem(
                title = item.getTitle(context),
                right = null,
                description = null,
                menuItem = item,
                isVisible = item.isVisible(0),
                badgeCount = item.getBadgeCount(),
            )
        }

        val views = RemoteViews(context.packageName, R.layout.app_widget_quick_access_item)
        views.setTextViewText(R.id.title, preparedItem.title)
        views.setInt(R.id.container, "setBackgroundResource", getBackground(item.style))
        views.setInt(R.id.icon, "setColorFilter", ContextCompat.getColor(context, item.style.highlightColor))
        views.setImageViewResource(R.id.icon, item.icon)
        views.setOnClickFillInIntent(R.id.container, ExecuteWidgetActionActivity.createClickMenuItemFillIntent(item.itemId))
        return views
    }

    private fun getBackground(style: MenuItemStyle) = when (style) {
        MenuItemStyle.Blue -> R.drawable.quick_access_item_background_printer
        MenuItemStyle.Green -> R.drawable.quick_access_item_background_octoprint
        MenuItemStyle.Neutral -> R.drawable.quick_access_item_background_neutral
        MenuItemStyle.OctoPrint -> R.drawable.quick_access_item_background_octoprint
        MenuItemStyle.Printer -> R.drawable.quick_access_item_background_printer
        MenuItemStyle.Red -> R.drawable.quick_access_item_background_support
        MenuItemStyle.Settings -> R.drawable.quick_access_item_background_settings
        MenuItemStyle.Support -> R.drawable.quick_access_item_background_support
        MenuItemStyle.Yellow -> R.drawable.quick_access_item_background_settings
    }
}