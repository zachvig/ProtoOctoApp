package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintBootingException(webUrl: HttpUrl) : OctoPrintException(webUrl = webUrl, userFacingMessage = "OctoPrint is still starting up")