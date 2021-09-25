package de.crysxd.octoapp.base.data.models

import com.google.gson.annotations.SerializedName

enum class WidgetType {
    @SerializedName("AnnouncementWidget")
    AnnouncementWidget,

    @SerializedName("ControlTemperatureWidget")
    ControlTemperatureWidget,

    @SerializedName("ExtrudeWidget")
    ExtrudeWidget,

    @SerializedName("GcodePreviewWidget")
    GcodePreviewWidget,

    @SerializedName("MoveToolWidget")
    MoveToolWidget,

    @SerializedName("PrePrintQuickAccessWidget")
    PrePrintQuickAccessWidget,

    @SerializedName("PrintQuickAccessWidget")
    PrintQuickAccessWidget,

    @SerializedName("ProgressWidget")
    ProgressWidget,

    @SerializedName("QuickAccessWidget")
    QuickAccessWidget,

    @SerializedName("SendGcodeWidget")
    SendGcodeWidget,

    @SerializedName("TuneWidget")
    TuneWidget,

    @SerializedName("WebcamWidget")
    WebcamWidget,
}