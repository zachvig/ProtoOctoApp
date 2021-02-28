package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Animatable2
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
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
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ui.ext.oneOffEndAction
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.main.MainMenu
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.android.synthetic.main.fragment_gcode_render.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.ref.WeakReference


class MenuBottomSheetFragment : BaseBottomSheetDialogFragment() {
    override val viewModel by injectViewModel<MenuBottomSheetViewModel>()
    private lateinit var viewBinding: MenuBottomSheetFragmentBinding
    private val adapter = Adapter()
    private val showLoadingRunnable = Runnable {
        viewBinding.loadingOverlay.isVisible = true
        viewBinding.loadingOverlay.animate().alpha(if (isLoading) 1f else 0f).withEndAction { viewBinding.loadingOverlay.isVisible = isLoading }.start()
    }
    private var isLoading = false
        set(value) {
            field = value
            if (value) {
                view?.postDelayed(showLoadingRunnable, 200L)
            } else {
                showLoadingRunnable.run()
                view?.removeCallbacks(showLoadingRunnable)
            }

        }
    var isCheckBoxChecked
        get() = viewBinding.checkbox.isChecked
        set(value) {
            viewBinding.checkbox.isChecked = value
        }

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
        viewModel.menuBackStack.add(settingsMenu)
        showMenu(settingsMenu)
    }

    fun onFavoriteChanged() {
        // We need to reload the main menu if a favorite was changed in case it was removed
        if (viewModel.menuBackStack.last() is MainMenu) {
            showMenu(viewModel.menuBackStack.last())
        }
    }

    private fun showMenu(settingsMenu: Menu) {
        val internal = suspend {
            try {
                // Run pre-show routine and load items
                isLoading = settingsMenu !is MainMenu
                if (settingsMenu.shouldShowMenu(this@MenuBottomSheetFragment)) {
                    val currentDestination = findNavController().currentDestination?.id ?: 0
                    val context = requireContext()
                    val items = withContext(Dispatchers.IO) {
                        settingsMenu.getMenuItem().map {
                            PreparedMenuItem(
                                menuItem = it,
                                title = it.getTitle(context),
                                description = it.getDescription(context),
                                isVisible = it.isVisible(currentDestination)
                            )
                        }.filter {
                            it.isVisible
                        }.sortedWith(compareBy<PreparedMenuItem> { it.menuItem.order }.thenBy { it.title.toString() })
                    }
                    val subtitle = settingsMenu.getSubtitle(requireContext())
                    val title = settingsMenu.getTitle(requireContext())

                    // Prepare animation
                    isLoading = false
                    viewBinding.bottom.movementMethod = null
                    beginDelayedTransition {
                        // Need to be applied after transition to prevent glitches
                        viewBinding.bottom.movementMethod = settingsMenu.getBottomMovementMethod(this@MenuBottomSheetFragment)
                    }

                    // Show menu
                    val emptyStateIcon = settingsMenu.getEmptyStateIcon()
                    val emptyStateAction = settingsMenu.getEmptyStateActionText(context)
                    val emptyStateUrl = settingsMenu.getEmptyStateActionUrl(context)
                    adapter.menuItems = items
                    viewBinding.emptyStateIcon.setImageResource(emptyStateIcon)
                    (viewBinding.emptyStateIcon.drawable as? Animatable)?.start()
                    viewBinding.emptyStateAction.text = emptyStateAction
                    viewBinding.emptyStateAction.setOnClickListener { Uri.parse(emptyStateUrl).open(context) }
                    viewBinding.emptyStateAction.isVisible = emptyStateAction != null && emptyStateUrl != null
                    viewBinding.emptyState.isVisible = emptyStateIcon != 0 && items.isEmpty()
                    viewBinding.recyclerView.isVisible = !viewBinding.emptyState.isVisible
                    viewBinding.title.text = title
                    viewBinding.title.isVisible = viewBinding.title.text.isNotBlank()
                    viewBinding.subtitle.text = settingsMenu.getEmptyStateSubtitle(context).takeIf { items.isEmpty() } ?: subtitle
                    viewBinding.subtitle.isVisible = viewBinding.subtitle.text.isNotBlank()
                    viewBinding.bottom.text = settingsMenu.getBottomText(requireContext())
                    viewBinding.bottom.isVisible = viewBinding.bottom.text.isNotBlank() && viewBinding.recyclerView.isVisible
                    viewBinding.checkbox.text = settingsMenu.getCheckBoxText(requireContext())
                    viewBinding.checkbox.isVisible = viewBinding.checkbox.text.isNotBlank()
                    viewBinding.checkbox.isChecked = false

                    // Update bottom sheet size
                    forceResizeBottomSheet()
                } else Unit
            } catch (e: Exception) {
                Timber.e(e, "Error while inflating menu")
                requireOctoActivity().showDialog(e)
                popMenu()
            }
        }

        // We don't want the loading state to flash in when opening main menu and we also don't need to
        // build it async -> run blocking for main menu
        if (settingsMenu is MainMenu) {
            Timber.i("Using blocking method to inflate main menu")
            runBlocking { internal() }
        } else {
            Timber.i("Using async method to inflate $settingsMenu")
            viewLifecycleOwner.lifecycleScope.launchWhenCreated { internal() }
        }
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

    private fun executeClick(item: MenuItem, holder: MenuItemHolder) {
        if (isLoading) return

        viewModel.execute {
            try {
                isLoading = true
                val before = viewModel.menuBackStack.last()
                item.onClicked(this@MenuBottomSheetFragment)
                val after = viewModel.menuBackStack.last()

                // We did not change the menu, the holder is still showing the same item and the OS is fancy
                // Play success animation
                if (after == before && holder.currentItem?.get() == item && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (holder.binding.successFeedback.drawable as? Animatable2)?.let {
                        holder.binding.successFeedback.isVisible = true
                        holder.binding.pin.animate().scaleX(0f).scaleY(0f).start()
                        holder.binding.icon.animate().scaleX(0f).scaleY(0f).setInterpolator(AccelerateInterpolator()).start()
                        it.oneOffEndAction {
                            holder.binding.pin.animate().scaleX(1f).scaleY(1f).setInterpolator(DecelerateInterpolator()).start()
                            holder.binding.icon.animate().scaleX(1f).scaleY(1f).setInterpolator(DecelerateInterpolator()).start()
                        }
                        it.start()
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    private inner class SpanSizeLookUp : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int) = if (adapter.menuItems[position].menuItem.showAsHalfWidth) 1 else 2
    }

    private data class PreparedMenuItem(
        val menuItem: MenuItem,
        val title: CharSequence,
        val description: CharSequence?,
        val isVisible: Boolean
    )

    private inner class Adapter : RecyclerView.Adapter<MenuItemHolder>() {
        var pinnedItemIds: Set<String> = emptySet()
        var menuItems: List<PreparedMenuItem> = emptyList()
            set(value) {
                pinnedItemIds = Injector.get().pinnedMenuItemsRepository().getPinnedMenuItems()
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MenuItemHolder(parent)

        override fun getItemCount() = menuItems.size

        override fun onBindViewHolder(holder: MenuItemHolder, position: Int) {
            val preparedItem = menuItems[position]
            val item = preparedItem.menuItem
            holder.currentItem = WeakReference(item)
            holder.binding.text.setText(preparedItem.title)
            holder.binding.description.text = preparedItem.description
            holder.binding.description.isVisible = holder.binding.description.text.isNotBlank()
            holder.binding.button.setOnClickListener {
                if (item is ToggleMenuItem) {
                    executeFlipToggle(holder, item)
                } else {
                    executeClick(item, holder)
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
                    menuItems = menuItems
                    onFavoriteChanged()
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

            // Margins
            val nextItem = menuItems.getOrNull(position + 1)?.menuItem
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
    }

    private inner class MenuItemHolder(parent: ViewGroup) :
        ViewBindingHolder<MenuItemBinding>(MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)) {
        var currentItem: WeakReference<MenuItem>? = null

        init {
            // In this list we don't recycle so we can use TransitionManager easily
            setIsRecyclable(false)
        }
    }
}