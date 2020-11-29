package de.crysxd.octoapp.base.gcode.parse

class CuraGcodeParser : GcodeParser() {
    override fun canParseFile(content: String) = content.contains(";Generated with Cura_SteamEngine")
    override fun isLayerChange(line: String) = line.startsWith(";LAYER:", true)
}