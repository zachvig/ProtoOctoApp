package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class BasicAuthRequiredException(val userRealm: String, webUrl: HttpUrl) : OctoPrintException(
    technicalMessage = "The server responded with 401, requesting authentication",
    userFacingMessage = "The server requires authentication. Enter your username and password or check that they are correct.<br><br><small>Server says: $userRealm</small>",
    webUrl = webUrl,
)
