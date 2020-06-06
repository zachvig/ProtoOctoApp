package de.crysxd.octoapp.octoprint.models.files

data class FileList(
    val files: List<FileObject>,
    val free: Long,
    val total: Long
)