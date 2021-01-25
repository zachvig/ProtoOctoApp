package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
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

    companion object {
        private const val KEY_MENU = "menu"
        fun createForMenu(menu: Menu) = MenuBottomSheetFragment().also {
            it.arguments = bundleOf(KEY_MENU to menu)
        }
    }

    private val rootMenu get() = arguments?.getParcelable<Menu>(KEY_MENU) ?: MainMenu()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        MenuBottomSheetFragmentBinding.inflate(inflater, container, false).also { viewBinding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2).also {
            it.spanSizeLookup = SpanSizeLookUp()
        }

        if (viewModel.menuBackStack.isEmpty()) {
            pushMenu(rootMenu)
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
        viewModel.menuBackStack.add(settingsMenu)
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

    private fun popMenu(): Boolean = if (viewModel.menuBackStack.size <= 1) {
        false
    } else {
        viewModel.menuBackStack.removeLast()
        showMenu(viewModel.menuBackStack.last())
        true
    }

    private fun beginDelayedTransition(smallChange: Boolean = false) {
        view?.findParent<CoordinatorLayout>()?.let {
            val epicenterX = it.width / 2
            val epicenterY = it.width / 2
            TransitionManager.beginDelayedTransition(
                it, InstantAutoTransition(
                    explode = !smallChange,
                    explodeEpicenter = Rect(epicenterX, epicenterY, epicenterX, epicenterY)
                )
            )
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
                }?.sortedWith(compareBy({ it.order }, { it.itemId })) ?: emptyList()
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
                    } else {
                        notifyDataSetChanged()
                    }
                }
            }
            if (!item.showAsSubMenu && item.canBePinned) {
                holder.binding.button.setOnLongClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        (requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                            .vibrate(VibrationEffect.createOneShot(30, 255))
                    }

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
                bottomMargin = requireContext().resources.getDimension(if (groupChanged) R.dimen.margin_1_2 else R.dimen.margin_0_1).toInt()
                marginEnd = requireContext().resources.getDimension(R.dimen.margin_0_1).toInt()
            }

            // Max lines
            holder.binding.button.maxLines = if (item.enforceSingleLine) 1 else 2

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