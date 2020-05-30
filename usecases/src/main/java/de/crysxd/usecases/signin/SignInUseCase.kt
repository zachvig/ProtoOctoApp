package de.crysxd.usecases.signin

import android.content.Context
import de.crysxd.models.SignInInformation
import de.crysxd.usecases.UseCase

class SignInUseCase(context: Context) : UseCase<SignInInformation, Unit> {

    override suspend fun execute(param: SignInInformation) {
        throw SignInException("Sign in is not implemented")
    }
}