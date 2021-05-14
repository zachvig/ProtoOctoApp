package de.crysxd.octoapp.octoprint.plugins.power.ws281x

sealed class WS281xCommand(val command: String) {
    object TurnTorchOn : WS281xCommand("torch_on")
    object TurnTorchOff : WS281xCommand("torch_off")
    object TurnLightsOn : WS281xCommand("lights_on")
    object TurnLightsOff : WS281xCommand("lights_off")
}
