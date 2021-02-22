package de.crysxd.octoapp.octoprint.exceptions

import java.net.URL

class BasicAuthRequiredException(userRealm: String) : OctoPrintException(
    technicalMessage = "The server responded with 401, requesting authentication",
    userFacingMessage = "The server requires authentication. Add username and password to the URL:\n\nhttp://<b>username:password</b>@host\n\n<small>Server message: $userRealm</small>",
    webUrl = null,
) {
    private var enrichedUserFacingMessage: String?

    init {
        enrichedUserFacingMessage = super.userFacingMessage
    }

    override val userFacingMessage: String?
        get() = enrichedUserFacingMessage

    fun enrichUserMessageWithUrl(url: URL) {
        enrichedUserFacingMessage = enrichedUserFacingMessage
            ?.replace("http://", "${url.protocol}://")
            ?.replace("@host", "@${url.host}${url.path}")
    }
}
