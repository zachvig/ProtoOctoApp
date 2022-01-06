package de.crysxd.octoapp.base.network

import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import java.io.IOException

class BrokenSetupException(
    val original: OctoPrintException,
    val isForPrimary: Boolean,
    val userMessage: String,
    val instance: OctoPrintInstanceInformationV3,
) : IOException("Broken setup: ${original.message}", original)