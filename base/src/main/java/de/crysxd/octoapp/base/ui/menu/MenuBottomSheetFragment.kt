package de.crysxd.octoapp.base.ui.menu

import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.Transition
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.MenuBottomSheetFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.main.MainMenu
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.android.synthetic.main.fragment_gcode_render.*
import kotlinx.coroutines.*
import timber.log.Timber


open class MenuBottomSheetFragment : BaseBottomSheetDialogFragment() {
    override val viewModel by injectViewModel<MenuBottomSheetViewModel>()
    private lateinit var viewBinding: MenuBottomSheetFragmentBinding
    private val adapter = MenuAdapter(::executeClick, ::executeLongClick)
    private val showLoadingRunnable = Runnable {
        viewBinding.loadingOverlay.isVisible = true
        viewBinding.loadingOverlay.animate().alpha(if (isLoading) 1f else 0f).withEndAction { viewBinding.loadingOverlay.isVisible = isLoading }.start()
    }
    private var lastClickedMenuItem: MenuItem? = null
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

    private fun showMenu(settingsMenu: Menu) {
        val internal = suspend {
            try {
                // Check if the menu wants to be shown (e.g. power menu can auto handle some requests)
                isLoading = settingsMenu !is MainMenu
                if (settingsMenu.shouldShowMenu(this@MenuBottomSheetFragment)) {
                    // Load items
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
                    viewBinding.emptyStateAction.setOnClickListener { Uri.parse(emptyStateUrl).open(requireOctoActivity()) }
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
                    lastClickedMenuItem = null

                    // Update bottom sheet size
                    forceResizeBottomSheet()
                } else {
                    abortShowMenu(false)
                    lastClickedMenuItem?.let { adapter.playSuccessAnimationForItem(it) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while inflating menu")
                requireOctoActivity().showDialog(e)
                abortShowMenu()
            } finally {
                isLoading = false
            }
        }

        // We don't want the loading state to flash in when opening main menu and we also don't need to
        // build it async -> run blocking for main menu
        if (settingsMenu.shouldLoadBlocking()) {
            Timber.i("Using blocking method to inflate main menu")
            runBlocking { internal() }
        } else {
            Timber.i("Using async method to inflate $settingsMenu")
            // First menu? no overlay background
            viewBinding.loadingOverlay.setBackgroundColor(
                ContextCompat.getColor(requireContext(), if (viewModel.menuBackStack.size == 1) android.R.color.transparent else R.color.black_translucent)
            )
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                internal()
                viewBinding.loadingOverlay.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black_translucent))
            }
        }
    }

    private fun abortShowMenu(showPrevious: Boolean = true) {
        if (!popMenu(showPrevious)) {
            dismissAllowingStateLoss()
        }
    }

    private fun popMenu(showPrevious: Boolean = true): Boolean = if (viewModel.menuBackStack.size <= 1) {
        false
    } else {
        viewModel.menuBackStack.removeLast()
        if (showPrevious) {
            showMenu(viewModel.menuBackStack.last())
        }
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
                    explodeEpicenter = Rect(epicenterX, epicenterY, epicenterX, epicenterY),
                    fadeText = !smallChange
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

    private fun executeLongClick(item: MenuItem) {
        val repo = Injector.get().pinnedMenuItemsRepository()
        repo.toggleMenuItemPinned(item.itemId)

        // We need to reload the main menu if a favorite was changed in case it was removed
        beginDelayedTransition(true)
        showMenu(viewModel.menuBackStack.last())
    }

    private fun executeClick(item: MenuItem) {
        if (isLoading) return

        viewModel.execute {
            try {
                isLoading = true
                lastClickedMenuItem = item
                if (item is ToggleMenuItem) {
                    item.handleToggleFlipped(this@MenuBottomSheetFragment, !item.isEnabled)
                    adapter.setToggle(item, item.isEnabled)
                } else {
                    val before = viewModel.menuBackStack.last()

                    item.onClicked(this@MenuBottomSheetFragment)

                    // We did not change the menu, the holder is still showing the same item and the OS is fancy
                    // Play success animation
                    val after = viewModel.menuBackStack.last()
                    if (after == before) {
                        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                            delay(100)
                            adapter.playSuccessAnimationForItem(item)
                        }
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
}