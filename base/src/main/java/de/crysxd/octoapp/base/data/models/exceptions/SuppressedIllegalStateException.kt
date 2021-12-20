package de.crysxd.octoapp.base.data.models.exceptions

import de.crysxd.octoapp.octoprint.exceptions.SuppressedException

class SuppressedIllegalStateException(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause), SuppressedException