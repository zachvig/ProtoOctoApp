package de.crysxd.octoapp.signin.models

import de.crysxd.octoapp.base.UserMessageException
import java.lang.Exception

class SignInException(override val userMessage: CharSequence) : Exception("Sign in failed"),
    UserMessageException