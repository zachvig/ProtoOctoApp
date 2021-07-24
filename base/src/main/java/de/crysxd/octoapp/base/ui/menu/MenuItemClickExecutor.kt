package de.crysxd.octoapp.base.ui.menu

import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.coroutines.delay

class MenuItemClickExecutor(
    private val host: MenuHost,
    private val adapter: MenuAdapter,
) : MenuHost by host {

    private var wasNewMenuPushed = false

    suspend fun execute(item: MenuItem) {
        wasNewMenuPushed = false
        when (item) {
            is ToggleMenuItem -> {
                item.handleToggleFlipped(this, !item.isEnabled)
                adapter.setToggle(item, item.isEnabled)
            }

            is RevolvingOptionsMenuItem -> {
                item.onClicked(this)
                adapter.updateMenuItem(
                    item = item,
                    startAnimation = {
                        TransitionManager.beginDelayedTransition(it, InstantAutoTransition(quickTransition = true))
                    },
                    update = {
                        it.copy(right = item.getRightDetail(requireContext()))
                    }
                )
            }

            else -> {
                item.onClicked(this)
                if (!wasNewMenuPushed) {
                    delay(100)
                    adapter.playSuccessAnimationForItem(item)
                }
            }
        }
    }

    override fun pushMenu(subMenu: Menu) {
        wasNewMenuPushed = true
        host.pushMenu(subMenu)
    }
}