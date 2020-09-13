package de.crysxd.octoapp.base.ext

import java.text.DecimalFormat

fun Long.asStyleFileSize(): String {
    val kb = this / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f

    return when {
        gb > 1 -> DecimalFormat("#.## 'GiB'").format(gb)
        mb > 1 -> DecimalFormat("#.# 'MiB'").format(mb)
        else -> DecimalFormat("# 'kiB'").format(kb)
    }
}