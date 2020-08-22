package de.crysxd.octoapp.base.ui.ext

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

suspend fun LayoutInflater.suspendedInflate(@LayoutRes layout: Int, parent: ViewGroup, addToParent: Boolean) = withContext(Dispatchers.Default) {
    try {
        inflate(layout, parent, addToParent)
    } catch (e: RuntimeException) {
        // Most likely handler
        Timber.w("Caught RuntimeException, re-attempting on main thread (message: ${e.message})")
        Timber.v(e)
        inflate(layout, parent, addToParent)
    }
}