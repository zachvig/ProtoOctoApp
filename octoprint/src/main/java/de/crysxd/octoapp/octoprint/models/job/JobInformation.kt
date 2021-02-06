package de.crysxd.octoapp.octoprint.models.job

import de.crysxd.octoapp.octoprint.models.files.FileObject

data class JobInformation(
    val file: FileObject.File
)