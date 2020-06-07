package de.crysxd.octoapp.octoprint.exceptions

import java.io.IOException
import java.lang.Exception

class OctoPrintApiException(responseCode: Int) : OctoPrintException(message = "Received unexpected response code $responseCode")