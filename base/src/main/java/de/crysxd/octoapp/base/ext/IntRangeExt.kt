package de.crysxd.octoapp.base.ext

fun IntRange.nextAfter(i: Int) = ((i + step) % last).coerceAtLeast(first)
