package de.crysxd.octoapp.octoprint.plugins.power.ocotrelay

import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.power.PowerPlugin
import okhttp3.HttpUrl

class OctoRelayPowerPlugin(
    private val octoRelayApi: OctoRelayApi
) : PowerPlugin<OctoRelayPowerDevice> {

    internal suspend fun toggle(pin: String) {
        octoRelayApi.sendCommand(OctoRelayCommand.Toggle(pin))
    }

    suspend fun getState(pin: String) = octoRelayApi.sendCommand(OctoRelayCommand.GetStatus(pin)).body()?.status

    private suspend fun getRawState(pin: String) = octoRelayApi.sendCommand(OctoRelayCommand.GetStatus(pin))

    suspend fun turnOff(pin: String) {
        val state = getRawState(pin)
        when (state.body()?.status) {
            true -> octoRelayApi.sendCommand(OctoRelayCommand.Toggle(pin))
            false -> Unit // Already off
            null -> throw UnknownStateException(state.raw().request.url)
        }
    }

    suspend fun turnOn(pin: String) {
        val state = getRawState(pin)
        when (state.body()?.status) {
            true -> Unit // Already on
            false -> octoRelayApi.sendCommand(OctoRelayCommand.Toggle(pin))
            null -> throw UnknownStateException(state.raw().request.url)
        }
    }

    override fun getDevices(settings: Settings) = settings.plugins.values.mapNotNull {
        it as? Settings.OctoRelaySettings
    }.map {
        it.devices?.map { it.copy(plugin = this) } ?: emptyList()
    }.flatten()

    class UnknownStateException(
        webUrl: HttpUrl,
    ) : OctoPrintException(
        userFacingMessage = "Unable to get device status, use toggle instead of turning the device on or off.",
        webUrl = webUrl
    )
}