package de.crysxd.baseui.menu

import android.annotation.SuppressLint
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
import de.crysxd.baseui.R
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.MenuItemBinding
import de.crysxd.baseui.ext.oneOffEndAction
import de.crysxd.octoapp.base.data.models.MenuId
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber
import java.lang.ref.WeakReference

@SuppressLint("NotifyDataSetChanged")
class MenuAdapter(
    private val onClick: (MenuItem) -> Unit,
    private val onSecondaryClick: (MenuItem) -> Unit = {},
    private val onPinItem: (MenuItem, View) -> Unit = { _, _ -> },
    private val menuId: MenuId = MenuId.Other,
) : RecyclerView.Adapter<MenuItemHolder>() {
    private var recyclerView: RecyclerView? = null

    private var pinnedItemIds: Set<String> = emptySet()
    var menuItems: List<PreparedMenuItem> = emptyList()
        set(value) {
            pinnedItemIds = BaseInjector.get().pinnedMenuItemsRepository().getPinnedMenuItems(menuId)
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


    @SuppressLint("MissingPermission")
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
        holder.binding.button.isEnabled = preparedItem.isEnabled
        holder.binding.button.setOnClickListener {
            onClick(item)
        }

        holder.binding.button.setOnLongClickListener {
            if (item.canBePinned) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                            .vibrate(VibrationEffect.createOneShot(30, 255))
                    } catch (e: SecurityException) {
                        Timber.w("Can't vibrate")
                    }
                }

                onPinItem(item, it)
            }
            true
        }

        // Toggle
        holder.binding.toggle.isVisible = item is ToggleMenuItem
        holder.binding.toggle.isChecked = (item as? ToggleMenuItem)?.isChecked == true
        holder.binding.toggle.isEnabled = preparedItem.isEnabled

        // Secondary button
        val icon = item.secondaryButtonIcon
        holder.binding.secondaryButton.isVisible = icon != null
        holder.binding.secondaryButton.setImageResource(icon ?: 0)
        holder.binding.secondaryButton.isEnabled = preparedItem.isEnabled
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

        // Badge
        holder.binding.badge.isVisible = preparedItem.badgeCount > 0
        holder.binding.badge.text = when (preparedItem.badgeCount) {
            0 -> ""
            in 1..9 -> preparedItem.badgeCount.toString()
            else -> "9+"
        }

        // Margins
        val nextItem = menuItems.getOrNull(position + 1)?.menuItem
        val groupChanged = nextItem != null && nextItem.groupId.isNotBlank() && nextItem.groupId != item.groupId
        val lastItem = nextItem == null
        var column = 0
        for (it in menuItems) {
            if (it.menuItem == item) break
            column = if (it.menuItem.showAsHalfWidth) (column + 1) % 2 else 0
        }
        holder.itemView.updateLayoutParams<GridLayoutManager.LayoutParams> {
            bottomMargin = when {
                lastItem -> null
                groupChanged -> R.dimen.margin_2
                else -> R.dimen.margin_0_1
            }?.let {
                context.resources.getDimension(it).toInt()
            } ?: 0
            marginEnd = if (item.showAsHalfWidth && column == 0) context.resources.getDimension(R.dimen.margin_0_1).toInt() else 0
            marginStart = if (item.showAsHalfWidth && column == 1) context.resources.getDimension(R.dimen.margin_0_1).toInt() else 0
        }

        // Colors
        val background = ColorStateList.valueOf(ContextCompat.getColor(context, item.style.backgroundColor))
        val foreground = ColorStateList.valueOf(ContextCompat.getColor(context, item.style.highlightColor))
        val transparent = ColorStateList.valueOf(Color.TRANSPARENT)
        holder.binding.secondaryButton.backgroundTintList = background
        holder.binding.secondaryButton.setColorFilter(foreground.defaultColor)
        holder.binding.button.backgroundTintList = if (item.showAsOutlined) transparent else background
        TextViewCompat.setCompoundDrawableTintList(holder.binding.button, foreground)
        TextViewCompat.setCompoundDrawableTintList(holder.binding.text, foreground)
        holder.binding.icon.setColorFilter(foreground.defaultColor)
        holder.binding.button.strokeColor = if (item.showAsOutlined) foreground else transparent
        holder.binding.button.rippleColor = if (item.showAsOutlined) foreground else background
        holder.binding.toggle.thumbDrawable.setTint(foreground.defaultColor)
        holder.binding.toggle.trackDrawable.setTint(background.defaultColor)
        holder.root.alpha = if (preparedItem.isEnabled) 1f else 0.5f
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
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val badgeCount: Int,
)
