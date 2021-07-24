package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.MenuItemBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ui.ext.oneOffEndAction
import java.lang.ref.WeakReference

class MenuAdapter(
    private val onClick: (MenuItem) -> Unit,
    private val onSecondaryClick: (MenuItem) -> Unit = {},
    private val onPinItem: (MenuItem, View) -> Unit = { _, _ -> },
) : RecyclerView.Adapter<MenuItemHolder>() {
    private var recyclerView: RecyclerView? = null

    var pinnedItemIds: Set<String> = emptySet()
    var menuItems: List<PreparedMenuItem> = emptyList()
        set(value) {
            pinnedItemIds = Injector.get().pinnedMenuItemsRepository().getPinnedMenuItems(MenuId.MainMenu)
            field = value
            notifyDataSetChanged()
        }

    fun setToggle(item: MenuItem, checked: Boolean) {
        val index = menuItems.indexOfFirst { item.itemId == it.menuItem.itemId }
        recyclerView?.layoutManager?.findViewByPosition(index)?.let {
            val binding = MenuItemBinding.bind(it)
            binding.toggle.isChecked = checked
        }
    }

    suspend fun updateMenuItem(item: MenuItem, startAnimation: (ViewGroup) -> Unit, update: suspend (PreparedMenuItem) -> PreparedMenuItem) {
        val index = menuItems.indexOfFirst { item.itemId == it.menuItem.itemId }
        recyclerView?.findViewHolderForAdapterPosition(index)?.let {
            (it as? MenuItemHolder)?.root?.let(startAnimation)
        }
        menuItems = menuItems.toMutableList().also {
            it[index] = update(it[index])
        }
    }

    fun playSuccessAnimationForItem(item: MenuItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val index = menuItems.indexOfFirst { item.itemId == it.menuItem.itemId }
            recyclerView?.layoutManager?.findViewByPosition(index)?.let {
                val binding = MenuItemBinding.bind(it)

                (binding.successFeedback.drawable as? Animatable2)?.let {
                    binding.successFeedback.isVisible = true
                    binding.pin.animate().scaleX(0f).scaleY(0f).start()
                    binding.icon.animate().scaleX(0f).scaleY(0f).setInterpolator(AccelerateInterpolator()).start()
                    it.oneOffEndAction {
                        binding.pin.animate().scaleX(1f).scaleY(1f).setInterpolator(DecelerateInterpolator()).start()
                        binding.icon.animate().scaleX(1f).scaleY(1f).setInterpolator(DecelerateInterpolator()).start()
                    }
                    it.start()
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemHolder(parent)

    override fun getItemCount() = menuItems.size


    override fun onBindViewHolder(holder: MenuItemHolder, position: Int) {
        val preparedItem = menuItems[position]
        val item = preparedItem.menuItem
        val context = holder.itemView.context

        holder.currentItem = WeakReference(item)
        holder.binding.text.text = preparedItem.title
        holder.binding.right.text = preparedItem.right
        holder.binding.right.isVisible = holder.binding.right.text.isNotBlank()
        holder.binding.description.text = preparedItem.description
        holder.binding.description.isVisible = holder.binding.description.text.isNotBlank()
        holder.binding.button.setOnClickListener {
            onClick(item)
        }

        holder.binding.button.setOnLongClickListener {
            if (item.canBePinned) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                        .vibrate(VibrationEffect.createOneShot(30, 255))
                }


                onPinItem(item, it)
            }
            true
        }

        // Toggle
        holder.binding.toggle.isVisible = item is ToggleMenuItem
        holder.binding.toggle.isChecked = (item as? ToggleMenuItem)?.isEnabled == true

        // Secondary button
        val icon = item.secondaryButtonIcon
        holder.binding.secondaryButton.isVisible = icon != null
        holder.binding.secondaryButton.setImageResource(icon ?: 0)
        holder.binding.secondaryButton.setOnClickListener {
            onSecondaryClick(item)
        }

        // Pin
        holder.binding.pin.isVisible = pinnedItemIds.contains(item.itemId)

        // Icons
        val iconStart = item.icon
        val iconEnd = R.drawable.ic_round_chevron_right_24.takeIf { item.showAsSubMenu } ?: 0
        holder.binding.text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconEnd, 0)
        holder.binding.icon.setImageResource(iconStart)
        holder.binding.icon.isVisible = iconStart != 0

        // Margins
        val nextItem = menuItems.getOrNull(position + 1)?.menuItem
        val groupChanged = nextItem != null && nextItem.groupId != item.groupId
        holder.itemView.updateLayoutParams<GridLayoutManager.LayoutParams> {
            bottomMargin = context.resources.getDimension(if (groupChanged) R.dimen.margin_2 else R.dimen.margin_0_1).toInt()
            marginEnd = context.resources.getDimension(R.dimen.margin_0_1).toInt()
        }

        // Colors
        val background = ColorStateList.valueOf(ContextCompat.getColor(context, item.style.backgroundColor))
        val foreground = ColorStateList.valueOf(ContextCompat.getColor(context, item.style.highlightColor))
        val transparent = ColorStateList.valueOf(Color.TRANSPARENT)
        holder.binding.secondaryButton.backgroundTintList = background
        holder.binding.secondaryButton.setColorFilter(foreground.defaultColor)
        holder.binding.button.backgroundTintList = if (item.showAsHalfWidth) transparent else background
        TextViewCompat.setCompoundDrawableTintList(holder.binding.button, foreground)
        TextViewCompat.setCompoundDrawableTintList(holder.binding.text, foreground)
        holder.binding.icon.setColorFilter(foreground.defaultColor)
        holder.binding.button.strokeColor = if (item.showAsHalfWidth) foreground else transparent
        holder.binding.button.rippleColor = if (item.showAsHalfWidth) foreground else background
    }
}

class MenuItemHolder(parent: ViewGroup) :
    ViewBindingHolder<MenuItemBinding>(MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)) {
    var currentItem: WeakReference<MenuItem>? = null
    val root = itemView as ViewGroup

    init {
        // In this list we don't recycle so we can use TransitionManager easily
        setIsRecyclable(false)
    }
}

data class PreparedMenuItem(
    val menuItem: MenuItem,
    val title: CharSequence,
    val right: CharSequence?,
    val description: CharSequence?,
    val isVisible: Boolean
)
