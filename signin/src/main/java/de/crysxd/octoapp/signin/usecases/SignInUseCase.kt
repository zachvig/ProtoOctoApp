package de.crysxd.octoapp.signin.usecases

import android.content.Context
import de.crysxd.octoapp.base.UseCase
import de.crysxd.octoapp.octoprint.Octoprint
import de.crysxd.octoapp.signin.models.SignInInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.net.InetAddress


class SignInUseCase(context: Context) : UseCase<SignInInformation, Unit> {

    override suspend fun execute(param: SignInInformation) = withContext(Dispatchers.IO) {
        val octoprint = Octoprint(
            param.ipAddress.toString(),
            param.port.toString().toInt(),
            param.apiKey.toString(),
            listOf(HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                   Timber.tag("HTTP").i(message)
                }
            }).setLevel(HttpLoggingInterceptor.Level.BODY))
        )

        Timber.i("Version: ${octoprint.createVersionApi().getVersion()}")
    }
}