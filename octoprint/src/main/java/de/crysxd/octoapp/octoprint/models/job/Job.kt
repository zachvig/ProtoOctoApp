package de.crysxd.octoapp.octoprint.models.job

import com.google.gson.annotations.SerializedName

data class Job(
    val progress: ProgressInformation?,
    @SerializedName("job") val info: JobInformation?
)