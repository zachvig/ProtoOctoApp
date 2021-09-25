package de.crysxd.baseui.menu

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.ext.toHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

abstract class ConfirmedMenuItem : MenuItem {
    abstract fun getConfirmMessage(context: Context): String
    abstract fun getConfirmPositiveAction(context: Context): CharSequence
    abstract suspend fun onConfirmed(host: MenuHost?)

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun onClicked(host: MenuHost?) = withContext(Dispatchers.Main) {
        var confirmed = false
        val latch = CountDownLatch(1)

        host ?: return@withContext

        // Show confirmation dialog
        MaterialAlertDialogBuilder(host.requireContext())
            .setMessage(getConfirmMessage(host.requireContext()).toHtml())
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
            host.suppressSuccessAnimationForNextAction()
        }
    }
}