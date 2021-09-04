package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class WebSocketUpgradeFailedException(val responseCode: Int, val webSocketUrl: HttpUrl, webUrl: HttpUrl) : OctoPrintException(
    webUrl = webUrl,
    technicalMessage = "The server responded with $responseCode when attempting to upgrade to web socket",
    userFacingMessage = "OctoApp attempted to establish a web socket connection, but the server responded with $responseCode.\n\n" +
            "This is a very common issue with reverse proxy setups. Please ensure your reverse proxy allows OctoApp to upgrade the HTTP connection for <b>$webSocketUrl</b> to the web socket protocol.\n\n" +
            "Usually, you need to manually add the 'Upgrade: WebSocket' header for the URL above as it is not forwarded to OctoPrint. Check out this examples " +
            "<a href=\"https://community.octoprint.org/t/reverse-proxy-configuration-examples/1107\">here</a>."
)