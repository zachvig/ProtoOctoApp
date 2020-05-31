package de.crysxd.octoapp.base.models.exceptions

import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UserMessageException
import de.crysxd.octoapp.octoprint.exceptions.PrinterNotOperationalException

class NoPrinterConnectedException : PrinterNotOperationalException(),
    UserMessageException {

    override val userMessage = R.string.error_no_printer_connected

}