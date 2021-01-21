package de.crysxd.octoapp.octoprint.exceptions

class WebSocketUpgradeFailedException(responseCode: Int, url: String) : OctoPrintException(
    message = "The server responded with $responseCode when attempting to upgrade to web socket",
    userFacingMessage = "OctoApp attempted to establish a web socket connection, but the server responded with $responseCode.\n\n" +
            "This is a very common issue with reverse proxy setups. Please ensure your reverse proxy allows OctoApp to upgrade the HTTP connection for <b>$url</b> to the web socket protocol.\n\n" +
            "Usually, you need to manually add the 'Upgrade: WebSocket' header for the URL above as it is not forwarded to OctoPrint. Check out this example for <a href=\"http://nginx.org/en/docs/http/websocket.html\">nginx</a>.\n\n" +
            "OctoApp heavily relies on the web socket connection for a snappy UI and can't function without it."
)