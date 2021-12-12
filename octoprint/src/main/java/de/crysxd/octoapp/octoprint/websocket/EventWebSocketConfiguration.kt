package de.crysxd.octoapp.octoprint.websocket

internal data class EventWebSocketConfiguration(
    val subscription: Subscribe = Subscribe(),
    val throttle: Throttle = Throttle(),
) {
    internal data class Subscribe(val subscribe: Subscription = Subscription())
    internal data class Throttle(val throttle: Int = 1)
}
