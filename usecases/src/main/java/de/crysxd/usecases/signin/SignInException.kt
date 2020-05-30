package de.crysxd.usecases.signin

import de.crysxd.models.UserMessageException
import java.lang.Exception

class SignInException(override val userMessage: CharSequence) : Exception("Sign in failed"),
    UserMessageException