package de.crysxd.octoapp.base.utils

import timber.log.Timber

inline fun <T> measureTime(traceName: String, block: () -> T): T {
    Timber.tag("Performance").i("[$traceName] started")
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()
    Timber.tag("Performance").i("[$traceName] completed in ${end - start}ms")
    return result
}