package de.crysxd.octoapp.base.ext

import android.net.Uri

fun Uri.Builder.resolve(path: String?) = this.apply {
    when {
        path == null -> Unit
        path.startsWith("/") -> appendEncodedPath(path.substring(1))
        else -> appendEncodedPath(path)
    }
}