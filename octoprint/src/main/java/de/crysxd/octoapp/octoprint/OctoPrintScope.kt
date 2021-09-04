package de.crysxd.octoapp.octoprint

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.plus
import java.util.logging.Level

internal val OctoPrintScope = GlobalScope + CoroutineExceptionHandler { _, throwable ->
    OctoPrintLogger.log(Level.SEVERE, "Caught NON-CONTAINED exception in OctoPrintScope!", throwable)
}