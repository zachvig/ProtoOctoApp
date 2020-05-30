package de.crysxd.octoapp.signin.usecases

import android.content.Context
import de.crysxd.octoapp.base.UseCase
import de.crysxd.octoapp.signin.models.SignInException
import de.crysxd.octoapp.signin.models.SignInInformation

class SignInUseCase(context: Context) : UseCase<SignInInformation, Unit> {

    override suspend fun execute(param: de.crysxd.octoapp.signin.models.SignInInformation) {
        throw SignInException("Sign in is not implemented")
    }
}