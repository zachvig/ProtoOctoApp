package de.crysxd.octoapp.base.utils

import timber.log.Timber

inline fun <T> measureTime(traceName: String, block: () -> T): T {
    Timber.tag("Performance").v("[$traceName] started")
    val start = System.currentTimeMillis()
    return try {
        block()
    } finally {
        val end = System.currentTimeMillis()
        Timber.tag("Performance").v("[$traceName] completed in ${end - start}ms")
    }
}