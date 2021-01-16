package de.crysxd.octoapp.octoprint.plugins.power.tasmota

sealed class TasmotaCommand(val command: String, val ip: String, val idx: String) {
    class TurnDeviceOn(device: TasmotaPowerDevice) : TasmotaCommand("turnOn", device.ip, device.idx)
    class TurnDeviceOff(device: TasmotaPowerDevice) : TasmotaCommand("turnOff", device.ip, device.idx)
    class GetDeviceStatus(device: TasmotaPowerDevice) : TasmotaCommand("checkStatus", device.ip, device.idx)
}

