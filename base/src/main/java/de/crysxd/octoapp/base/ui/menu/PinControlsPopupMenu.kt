package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.main.MenuItemLibrary


class PinControlsPopupMenu(private val context: Context, private val menuId: MenuId) {

    fun show(itemId: String, anchor: View, onDone: () -> Unit = {}) {
        val repo = Injector.get().pinnedMenuItemsRepository()

        // Build menu
        val menu = PopupMenu(anchor.context, anchor)
        val menuItem = MenuItemLibrary().get(itemId) ?: return
        repo.checkPinnedState(itemId).sortedBy {
            // Sort current menu up
            if (it.first == menuId) "0${it.first}" else "1${it.first}"
        }.filter {
            it.first.canPin(menuItem)
        }.forEach {
            // Create text
            val text = context.getString(
                if (it.second) R.string.menu_controls___unpin_from_x else R.string.menu_controls___pin_to_x,
                context.getString(it.first.label),
            )
            val spanned = SpannableString("  $text")

            // Load icon
            if (it.first == menuId) {
                ContextCompat.getDrawable(context, R.drawable.ic_round_push_pin_16)
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_empty_16)
            }?.also { drawable ->
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                val span = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
                spanned.setSpan(span, 0, 1, SpannableString.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Add to menu
            menu.menu.add(android.view.Menu.NONE, it.first.ordinal, android.view.Menu.NONE, spanned)
        }

        // Show menu
        menu.show()
        menu.setOnMenuItemClickListener {
            val menuId = MenuId.values()[it.itemId]
            repo.toggleMenuItemPinned(menuId, itemId)
            onDone()
            true
        }
    }
}