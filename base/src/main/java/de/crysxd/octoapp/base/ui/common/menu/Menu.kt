package de.crysxd.octoapp.base.ui.common.menu

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

interface Menu {
    fun getMenuItem(): List<MenuItem>
    fun getTitle(context: Context): CharSequence? = null
    fun getSubtitle(context: Context): CharSequence? = null
    fun getBottomText(context: Context): CharSequence? = null
}

interface MenuItem {
    val itemId: String
    val groupId: String
    val order: Int

    val style: Style
    val showAsSubMenu: Boolean get() = false
    val showAsHalfWidth: Boolean get() = false

    @get:DrawableRes
    val icon: Int

    suspend fun getTitle(context: Context): CharSequence
    suspend fun isVisible(@IdRes destinationId: Int) = true
    suspend fun onClicked(host: MenuBottomSheetFragment) = true
}

abstract class SubMenuItem : MenuItem {
    abstract val subMenu: Menu

    override suspend fun isVisible(@IdRes destinationId: Int) = subMenu.getMenuItem().any { it.isVisible(destinationId) }
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean {
        host.pushMenu(subMenu)
        return false
    }
}

abstract class ConfirmedMenuItem : MenuItem {
    abstract fun getConfirmMessage(context: Context): CharSequence
    abstract fun getConfirmPositiveAction(context: Context): CharSequence
    abstract suspend fun onConfirmed(host: MenuBottomSheetFragment): Boolean

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun onClicked(host: MenuBottomSheetFragment): Boolean = withContext(Dispatchers.Main) {
        var confirmed = false
        val latch = CountDownLatch(1)

        // Show confirmation dialog
        MaterialAlertDialogBuilder(host.requireContext())
            .setMessage(getConfirmMessage(host.requireContext()))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(getConfirmPositiveAction(host.requireContext())) { _, _ ->
                confirmed = true
            }.setOnDismissListener {
                latch.countDown()
            }
            .show()

        // Wait for the dialog to be closed :)
        withContext(Dispatchers.IO) { latch.await() }

        // Run action
        if (confirmed) {
            onConfirmed(host)
        } else {
            false
        }
    }
}

sealed class Style(
    @ColorRes val backgroundColor: Int,
    @ColorRes val highlightColor: Int,
) {
    object Support : Style(R.color.red_translucent, R.color.red)
    object Settings : Style(R.color.yellow_translucent, R.color.yellow)
    object Routines : Style(R.color.green_translucent, R.color.green)
    object Printer : Style(R.color.blue_translucent, R.color.blue)
    object Neutral : Style(R.color.light_grey_translucent, R.color.light_grey)
}