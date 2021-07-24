package de.crysxd.octoapp.base.ui.widget.quickaccess

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.QuickAccessWidgetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.base.ui.menu.MenuAdapter
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.PinControlsPopupMenu
import de.crysxd.octoapp.base.ui.menu.PreparedMenuItem
import de.crysxd.octoapp.base.ui.menu.main.MenuItemLibrary
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget

abstract class QuickAccessWidget(context: Context) : RecyclableOctoWidget<QuickAccessWidgetBinding, QuickAccessWidgetViewModel>(context) {

    abstract val menuId: MenuId
    abstract val currentNavDestination: Int

    override val binding = QuickAccessWidgetBinding.inflate(LayoutInflater.from(context))
    private val library = MenuItemLibrary()
    private val adapter = MenuAdapter(
        onClick = ::onMenuItemClicked,
        onPinItem = ::onShowPinMenu
    ).also {
        binding.recyclerView.adapter = it
    }

    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<QuickAccessWidgetViewModel>().value
    override fun getTitle(context: Context) = context.getString(R.string.widget_quick_access)
    override fun getAnalyticsName() = "quick_access"
    override fun isVisible() = baseViewModel.hasAny(menuId)
    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)

        baseViewModel.load(menuId).observe(lifecycleOwner) {
            // Start animation so the update is smooooth
            parent.requestTransition(quickTransition = true)

            lifecycleOwner.lifecycleScope.launchWhenCreated {
                adapter.pinnedItemIds = it
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

    }
}