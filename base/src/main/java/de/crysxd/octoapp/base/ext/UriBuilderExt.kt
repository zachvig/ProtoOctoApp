package de.crysxd.octoapp.base.ext

import android.net.Uri

fun Uri.Builder.appendFullPath(path: String?) = this.apply {
    path?.split("/")?.forEach { appendPath(it) }
}