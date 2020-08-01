package de.crysxd.octoapp.octoprint.models.files

data class FileList(
    val files: List<FileObject>,
    val free: Long? = null,
    val total: Long? = null
)