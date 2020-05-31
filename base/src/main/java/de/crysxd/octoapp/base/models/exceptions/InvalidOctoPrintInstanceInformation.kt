package de.crysxd.octoapp.base.models.exceptions

import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UserMessageException
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.PrinterNotOperationalException

class InvalidOctoPrintInstanceInformation : InvalidApiKeyException(),
    UserMessageException {

    override val userMessage = R.string.error_invalid_octoprint_instance_information

}