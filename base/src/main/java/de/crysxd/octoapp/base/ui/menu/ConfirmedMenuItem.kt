package de.crysxd.octoapp.base.ui.menu

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch

abstract class ConfirmedMenuItem : MenuItem {
    abstract fun getConfirmMessage(context: Context): CharSequence
    abstract fun getConfirmPositiveAction(context: Context): CharSequence
    abstract suspend fun onConfirmed(host: MenuBottomSheetFragment)

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun onClicked(host: MenuBottomSheetFragment) = withContext(Dispatchers.Main) {
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
        }
    }
}