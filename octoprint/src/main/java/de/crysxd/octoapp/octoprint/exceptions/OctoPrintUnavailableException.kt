package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

class OctoPrintUnavailableException(e: Exception? = null, webUrl: HttpUrl) : OctoPrintException(
    cause = e?.let { ProxyException.create(it, webUrl.toString()) },
    userFacingMessage = e?.localizedMessage ?: e?.message ?: "Unable to connect to OctoPrint"
)