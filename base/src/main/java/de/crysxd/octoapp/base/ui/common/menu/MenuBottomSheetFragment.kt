package de.crysxd.octoapp.base.ui.common.menu

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.MenuBottomSheetFragmentBinding
import de.crysxd.octoapp.base.databinding.MenuItemBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder


class MenuBottomSheetFragment : BaseBottomSheetDialogFragment() {
    override val viewModel by injectViewModel<MenuBottomSheetViewModel>()
    private lateinit var viewBinding: MenuBottomSheetFragmentBinding
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pushMenu(MainMenu())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        MenuBottomSheetFragmentBinding.inflate(inflater, container, false).also { viewBinding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2).also {
            it.spanSizeLookup = SpanSizeLookUp()
        }


    }

    fun show(fm: FragmentManager) = show(fm, "main-menu")

    fun pushMenu(settingsMenu: Menu) {
        adapter.menu?.let { viewModel.menuBackStack.add(it) }
        adapter.menu = settingsMenu
    }

    fun popMenu(): Boolean = if (viewModel.menuBackStack.isEmpty()) {
        false
    } else {
        adapter.menu = viewModel.menuBackStack.removeLast()
        true
    }

    private inner class SpanSizeLookUp : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) = if (adapter.menuItems[position].showAsHalfWidth) 1 else 2
    }

    private inner class Adapter : RecyclerView.Adapter<MenuItemHolder>() {
        var menu: Menu? = null
            set(value) {
                field = value
                val currentDestination = findNavController().currentDestination?.id ?: 0
                menuItems = value?.getMenuItem(requireContext())?.filter { it.isVisible(currentDestination) } ?: emptyList()
                notifyDataSetChanged()
            }

        var menuItems: List<MenuItem> = emptyList()
            private set

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemHolder(parent)

        override fun getItemCount() = menuItems.size

        override fun onBindViewHolder(holder: MenuItemHolder, position: Int) {
            val item = menuItems[position]
            holder.binding.button.text = item.title
            holder.binding.button.setOnClickListener { item.onClicked(this@MenuBottomSheetFragment) }

            // Icons
            holder.binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                item.icon,
                0,
                R.drawable.ic_round_chevron_right_24.takeIf { item.showAsSubMenu } ?: 0,
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
        ViewBindingHolder<MenuItemBinding>(MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

}