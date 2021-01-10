package de.crysxd.octoapp.base.ui.common.menu

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.MenuBottomSheetFragmentBinding
import de.crysxd.octoapp.base.databinding.MenuItemBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ui.common.menu.main.MainMenu
import de.crysxd.octoapp.base.ui.ext.findParent
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


class MenuBottomSheetFragment : BaseBottomSheetDialogFragment() {
    override val viewModel by injectViewModel<MenuBottomSheetViewModel>()
    private lateinit var viewBinding: MenuBottomSheetFragmentBinding
    private val adapter = Adapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        MenuBottomSheetFragmentBinding.inflate(inflater, container, false).also { viewBinding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2).also {
            it.spanSizeLookup = SpanSizeLookUp()
        }

        if (viewModel.menuBackStack.isEmpty()) {
            pushMenu(MainMenu())
        } else {
            showMenu(viewModel.menuBackStack.last())
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = super.onCreateDialog(savedInstanceState).also {
        it.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && !popMenu()) {
                dismiss()
            }
            true
        }
    }

    fun show(fm: FragmentManager) = show(fm, "main-menu")

    fun pushMenu(settingsMenu: Menu) {
        adapter.menu?.let { viewModel.menuBackStack.add(it) }
        showMenu(settingsMenu)
    }

    fun showMenu(settingsMenu: Menu) {
        beginDelayedTransition()
        adapter.menu = settingsMenu
        viewBinding.title.text = settingsMenu.getTitle(requireContext())
        viewBinding.title.isVisible = viewBinding.title.text.isNotBlank()
        viewBinding.subtitle.text = settingsMenu.getSubtitle(requireContext())
        viewBinding.subtitle.isVisible = viewBinding.subtitle.text.isNotBlank()
        viewBinding.bottom.text = settingsMenu.getBottomText(requireContext())
        viewBinding.bottom.isVisible = viewBinding.bottom.text.isNotBlank()
    }

    private fun popMenu(): Boolean = if (viewModel.menuBackStack.isEmpty()) {
        false
    } else {
        showMenu(viewModel.menuBackStack.removeLast())
        true
    }

    private fun beginDelayedTransition(smallChange: Boolean = false) {
        view?.findParent<CoordinatorLayout>()?.let {
            TransitionManager.beginDelayedTransition(it, InstantAutoTransition(explode = !smallChange))
        }
    }

    private inner class SpanSizeLookUp : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) = if (adapter.menuItems[position].showAsHalfWidth) 1 else 2
    }

    private inner class Adapter : RecyclerView.Adapter<MenuItemHolder>() {
        var menu: Menu? = null
            set(value) {
                field = value
                val currentDestination = findNavController().currentDestination?.id ?: 0
                pinnedItemIds = Injector.get().pinnedMenuItemsRepository().getPinnedMenuItems()
                menuItems = value?.getMenuItem()?.filter {
                    runBlocking {
                        it.isVisible(currentDestination)
                    }
                } ?: emptyList()
                notifyDataSetChanged()
            }

        var pinnedItemIds: Set<String> = emptySet()
        var menuItems: List<MenuItem> = emptyList()
            private set

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemHolder(parent)

        override fun getItemCount() = menuItems.size

        override fun onBindViewHolder(holder: MenuItemHolder, position: Int) {
            val item = menuItems[position]
            holder.binding.button.text = runBlocking { item.getTitle(requireContext()) }
            holder.binding.button.setOnClickListener {
                viewModel.execute {
                    delay(100)
                    if (item.onClicked(this@MenuBottomSheetFragment)) {
                        dismiss()
                    }
                }
            }
            if (!item.showAsSubMenu) {
                holder.binding.button.setOnLongClickListener {
                    beginDelayedTransition(true)
                    Injector.get().pinnedMenuItemsRepository().toggleMenuItemPinned(item.itemId)
                    menu = menu
                    true
                }
            }

            // Icons
            holder.binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                item.icon,
                0,
                R.drawable.ic_round_chevron_right_24.takeIf { item.showAsSubMenu }
                    ?: R.drawable.ic_round_push_pin_16_half_alpha.takeIf { pinnedItemIds.contains(item.itemId) }
                    ?: 0,
                0
            )

            // Margins
            val nextItem = menuItems.getOrNull(position + 1)
            val groupChanged = nextItem != null && nextItem.groupId != item.groupId
            holder.itemView.updateLayoutParams<GridLayoutManager.LayoutParams> {
                bottomMargin = requireContext().resources.getDimension(if (groupChanged) R.dimen.margin_2 else R.dimen.margin_0).toInt()
            }

            // Colors
            val background = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), item.style.backgroundColor))
            val foreground = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), item.style.highlightColor))
            val transparent = ColorStateList.valueOf(Color.TRANSPARENT)
            holder.binding.button.backgroundTintList = if (item.showAsHalfWidth) transparent else background
            TextViewCompat.setCompoundDrawableTintList(holder.binding.button, foreground)
            holder.binding.button.strokeColor = if (item.showAsHalfWidth) foreground else transparent
            holder.binding.button.rippleColor = if (item.showAsHalfWidth) foreground else background
        }
    }

    private inner class MenuItemHolder(parent: ViewGroup) :
        ViewBindingHolder<MenuItemBinding>(MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)) {
        init {
            // In this list we don't recycle so we can use TransitionManager easily
            setIsRecyclable(false)
        }
    }
}