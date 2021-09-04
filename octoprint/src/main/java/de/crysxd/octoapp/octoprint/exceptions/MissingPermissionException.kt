package de.crysxd.octoapp.octoprint.exceptions

import okhttp3.HttpUrl

open class MissingPermissionException(httpUrl: HttpUrl) : OctoPrintException(
    webUrl = httpUrl,
    technicalMessage = "OctoPrint returned 403 for $httpUrl but the API key is valid. This indicates a missing permission.",
    userFacingMessage = "OctoPrint did not allow access to a function. This is caused by the current API key used by OctoApp missing a required permission."
)