package de.crysxd.octoapp.base.ui.widget.quickaccess

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.QuickAccessWidgetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.main.MenuItemLibrary
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget

abstract class QuickAccessWidget(
    context: Context
) : RecyclableOctoWidget<QuickAccessWidgetBinding, QuickAccessWidgetViewModel>(context), MenuHost {

    abstract val menuId: MenuId
    abstract val currentNavDestination: Int
    private var suppressSuccessAnimation = false

    override val binding = QuickAccessWidgetBinding.inflate(LayoutInflater.from(context))
    private val library = MenuItemLibrary()
    private val adapter by lazy {
        MenuAdapter(
            onClick = ::onMenuItemClicked,
            onPinItem = ::onShowPinMenu,
            menuId = menuId
        ).also {
            binding.recyclerView.adapter = it
        }
    }

    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<QuickAccessWidgetViewModel>().value
    override fun getTitle(context: Context) = context.getString(R.string.widget_quick_access)
    override fun getAnalyticsName() = "quick_access"
    override fun isVisible() = baseViewModel.hasAny(menuId)
    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)

        binding.tutorial.onLearnMoreAction = {
            Uri.parse(parent.getString(R.string.quick_access_tutorial_learn_more_link)).open(parent.requireActivity())
        }

        baseViewModel.executing.observe(lifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            binding.loadingOverlay.setOnClickListener { /* Block clicks */ }
            binding.loadingOverlay.isVisible = it
        }

        baseViewModel.load(menuId).observe(lifecycleOwner) {
            // Start animation so the update is smooooth
            parent.requestTransition(quickTransition = true)

            lifecycleOwner.lifecycleScope.launchWhenCreated {
                adapter.menuItems = it.mapNotNull {
                    library[it]
                }.sortedBy {
                    it.order
                }.map {
                    PreparedMenuItem(
                        menuItem = it,
                        title = it.getTitle(context),
                        right = it.getRightDetail(context),
                        description = it.getDescription(context),
                        isVisible = it.isVisible(currentNavDestination)
                    )
                }
            }
        }
    }

    private fun onShowPinMenu(menuItem: MenuItem, anchor: View) = PinControlsPopupMenu(context, menuId).show(menuItem.itemId, anchor)

    private fun onMenuItemClicked(menuItem: MenuItem) {
        binding.loadingOverlay.setOnClickListener {}
        baseViewModel.execute {
            MenuItemClickExecutor(this@QuickAccessWidget, adapter).execute(menuItem)
        }
    }

    override fun requireContext() = parent.requireContext()

    override fun pushMenu(subMenu: Menu) {
        MenuBottomSheetFragment.createForMenu(subMenu).show(getMenuFragmentManager())
    }

    override fun closeMenu() = Unit

    override fun getNavController() = parent.findNavController()

    override fun getMenuActivity() = parent.requireOctoActivity()

    override fun getMenuFragmentManager() = parent.childFragmentManager

    override fun getHostFragment() = parent

    override fun reloadMenu() {
        parent.requestTransition(quickTransition = true)
        adapter.menuItems = adapter.menuItems
    }

    override fun isCheckBoxChecked() = false

    override fun consumeSuccessAnimationForNextActionSuppressed(): Boolean {
        val value = suppressSuccessAnimation
        suppressSuccessAnimation = false
        return value
    }

    override fun suppressSuccessAnimationForNextAction() {
        suppressSuccessAnimation = true
    }
}