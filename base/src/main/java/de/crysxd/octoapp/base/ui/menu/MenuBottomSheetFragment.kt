package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.MenuBottomSheetFragmentBinding
import de.crysxd.octoapp.base.databinding.MenuItemBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.OctoActivity
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.main.MainMenu
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.coroutines.*
import timber.log.Timber


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
                dismissAllowingStateLoss()
            }
            true
        }
    }

    fun show(fm: FragmentManager) = show(fm, "main-menu")

    fun pushMenu(settingsMenu: Menu) {
        // Samsung Android 11 decides to crash if we trigger this method
        // from a link click, posting resolves this issue
        view?.post {
            if (isAdded) {
                viewModel.menuBackStack.add(settingsMenu)
                showMenu(settingsMenu)
            }
        }
    }

    fun showMenu(settingsMenu: Menu) {
        viewBinding.bottom.movementMethod = null
        beginDelayedTransition {
            // Need to be applied after transition to prevent glitches
            viewBinding.bottom.movementMethod = settingsMenu.getBottomMovementMethod(this)
        }

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

    private fun beginDelayedTransition(smallChange: Boolean = false, endAction: () -> Unit = {}) {
        view?.rootView?.let {
            // We need a offset if the view does not span the entire screen as the epicenter is in screen coordinates (?)
            val epicenterX = getScreenWidth() / 2
            val epicenterY = it.width / 2
            TransitionManager.beginDelayedTransition(
                it as ViewGroup,
                InstantAutoTransition(
                    explode = !smallChange,
                    explodeEpicenter = Rect(epicenterX, epicenterY, epicenterX, epicenterY)
                ).also { t ->
                    t.addListener(
                        object : Transition.TransitionListener {
                            override fun onTransitionStart(transition: Transition) = Unit
                            override fun onTransitionCancel(transition: Transition) = Unit
                            override fun onTransitionPause(transition: Transition) = Unit
                            override fun onTransitionResume(transition: Transition) = Unit
                            override fun onTransitionEnd(transition: Transition) = endAction()
                        }
                    )
                }
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
            val title = runBlocking { item.getTitle(requireContext()) }
            holder.binding.text.text = title
            holder.binding.description.text = runBlocking { item.getDescription(requireContext()) }
            holder.binding.description.isVisible = holder.binding.description.text.isNotBlank()
            holder.binding.button.setOnClickListener {
                if (item is ToggleMenuItem) {
                    executeFlipToggle(holder, item)
                } else {
                    executeClick(item, title)
                }
            }

            if (item.canBePinned) {
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

            // Toggle
            holder.binding.toggle.isVisible = item is ToggleMenuItem
            holder.binding.toggle.isChecked = (item as? ToggleMenuItem)?.isEnabled == true

            // Pin
            holder.binding.pin.isVisible = pinnedItemIds.contains(item.itemId)

            // Icons
            val iconStart = item.icon
            val iconEnd = R.drawable.ic_round_chevron_right_24.takeIf { item.showAsSubMenu } ?: 0
            holder.binding.text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconEnd, 0)
            holder.binding.icon.setImageResource(iconStart)
            //  holder.binding.pin.setImageResource(iconEnd)

            // Margins
            val nextItem = menuItems.getOrNull(position + 1)
            val groupChanged = nextItem != null && nextItem.groupId != item.groupId
            holder.itemView.updateLayoutParams<GridLayoutManager.LayoutParams> {
                bottomMargin = requireContext().resources.getDimension(if (groupChanged) R.dimen.margin_2 else R.dimen.margin_0_1).toInt()
                marginEnd = requireContext().resources.getDimension(R.dimen.margin_0_1).toInt()
            }

            // Colors
            val background = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), item.style.backgroundColor))
            val foreground = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), item.style.highlightColor))
            val transparent = ColorStateList.valueOf(Color.TRANSPARENT)
            holder.binding.button.backgroundTintList = if (item.showAsHalfWidth) transparent else background
            TextViewCompat.setCompoundDrawableTintList(holder.binding.button, foreground)
            TextViewCompat.setCompoundDrawableTintList(holder.binding.text, foreground)
            holder.binding.icon.setColorFilter(foreground.defaultColor)
            holder.binding.button.strokeColor = if (item.showAsHalfWidth) foreground else transparent
            holder.binding.button.rippleColor = if (item.showAsHalfWidth) foreground else background
        }

        private fun executeFlipToggle(holder: MenuItemHolder, item: ToggleMenuItem) {
            viewModel.execute {
                try {
                    item.handleToggleFlipped(this@MenuBottomSheetFragment, !item.isEnabled)
                    holder.binding.toggle.isChecked = item.isEnabled
                } catch (e: Exception) {
                    requireOctoActivity().showDialog(e)
                }
            }
        }

        private fun executeClick(item: MenuItem, title: CharSequence) {
            val activity = requireOctoActivity()
            viewModel.execute {
                val closeBottomSheet = item.onClicked(this@MenuBottomSheetFragment) { action ->
                    GlobalScope.launch {
                        delay(100)

                        try {
                            // Start confirmation
                            Timber.i("Action start")
                            activity.showSnackbar(
                                OctoActivity.Message.SnackbarMessage(
                                    text = { it.getString(R.string.menu___executing_command, title) },
                                    debounce = true
                                )
                            )

                            // Execute
                            action()

                            // End confirmation
                            Timber.i("Action end")
                            activity.showSnackbar(
                                OctoActivity.Message.SnackbarMessage(
                                    text = { it.getString(R.string.menu___completed_command, title) },
                                    type = OctoActivity.Message.SnackbarMessage.Type.Positive
                                )
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Action failed")
                            activity.showDialog(e)
                        }
                    }
                }

                if (closeBottomSheet) {
                    dismiss()
                }
            }
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