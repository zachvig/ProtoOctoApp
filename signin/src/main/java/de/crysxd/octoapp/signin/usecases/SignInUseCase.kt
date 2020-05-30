package de.crysxd.octoapp.signin.usecases

import android.content.Context
import de.crysxd.octoapp.base.UseCase
import de.crysxd.octoapp.signin.models.SignInInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.octoprint.api.OctoPrintInstance
import org.octoprint.api.PrinterCommand
import timber.log.Timber


class SignInUseCase(context: Context) : UseCase<SignInInformation, Unit> {

    override suspend fun execute(param: SignInInformation) = withContext(Dispatchers.IO) {
        val octoprint = OctoPrintInstance(
            param.ipAddress.toString(),
            param.port.toString().toInt(),
            param.apiKey.toString()
        )

        val printer = PrinterCommand(octoprint)

        Timber.i("State: ${printer.bedTemp}")
    }
}