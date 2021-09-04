package de.crysxd.octoapp.base.utils

import de.crysxd.octoapp.base.ui.base.OctoActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

private val appJob = SupervisorJob()
val AppScope = CoroutineScope(appJob + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, throwable ->
    Timber.e(throwable, "Caught NON-CONTAINED exception in AppScope!")
    OctoActivity.instance?.showDialog(throwable)
})