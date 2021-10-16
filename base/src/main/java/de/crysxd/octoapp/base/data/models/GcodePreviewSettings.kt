package de.crysxd.octoapp.base.data.models

data class GcodePreviewSettings(
    val showPreviousLayer: Boolean = false,
    val showCurrentLayer: Boolean = false,
    val quality: Quality = Quality.Medium,
) {
    enum class Quality {
        Low, Medium, Ultra
    }
}