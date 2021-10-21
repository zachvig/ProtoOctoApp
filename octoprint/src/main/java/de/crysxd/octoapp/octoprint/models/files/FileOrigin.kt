package de.crysxd.octoapp.octoprint.models.files

import com.google.gson.annotations.SerializedName

enum class FileOrigin {
    @SerializedName("local")
    Local,

    @SerializedName("sdcard")
    SdCard;

    override fun toString() = when (this) {
        Local -> "local"
        SdCard -> "sdcard"
    }
}
