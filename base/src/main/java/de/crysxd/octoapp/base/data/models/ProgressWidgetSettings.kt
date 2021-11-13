package de.crysxd.octoapp.base.data.models

data class ProgressWidgetSettings(
    val showUsedTime: Boolean = false,
    val showLeftTime: Boolean = false,
    val showThumbnail: Boolean = false,
    val etaStyle: EtaStyle = EtaStyle.Compact,
    val printNameStyle: PrintNameStyle = PrintNameStyle.Compact,
    val fontSize: FontSize = FontSize.Normal,
) {
    enum class FontSize {
        Small, Normal
    }

    enum class PrintNameStyle {
        None, Compact, Full
    }

    enum class EtaStyle {
        None, Compact, Full
    }
}