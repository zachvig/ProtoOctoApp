package de.crysxd.octoapp.base.ext

val String.isHlsStreamUrl get() = endsWith(".m3u") || endsWith(".m3u8")