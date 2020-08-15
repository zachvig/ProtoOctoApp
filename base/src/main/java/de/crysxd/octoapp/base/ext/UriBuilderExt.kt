package de.crysxd.octoapp.base.ext

import android.net.Uri

fun Uri.Builder.resolve(path: String?) = this.apply {
    val (cleanedPath, query) = (path?.plus("?") ?: "?").split("?")
    cleanedPath.split("/").forEach { appendPath(it) }
    query(query)
}