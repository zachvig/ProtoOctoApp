package de.crysxd.octoapp.base.utils

import timber.log.Timber

inline fun <T> measureTime(traceName: String, block: () -> T): T {
    Timber.tag("Performance").v("[$traceName] started")
    val start = System.nanoTime()
    return try {
        block()
    } finally {
        val end = System.nanoTime()
        Timber.tag("Performance").v("[$traceName] completed in %.3f ms", (end - start) / 1_000_000f)
    }
}