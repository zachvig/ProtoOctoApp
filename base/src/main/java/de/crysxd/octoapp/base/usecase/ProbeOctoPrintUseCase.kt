package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import timber.log.Timber
import java.security.cert.Certificate
import javax.inject.Inject

class ProbeOctoPrintUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<ProbeOctoPrintUseCase.Params, List<ProbeOctoPrintUseCase.Finding>>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree): List<Finding> {
        val instance = octoPrintProvider.createAdHocOctoPrint(OctoPrintInstanceInformationV2(webUrl = param.webUrl, apiKey = "thisisnotaapikey"))

        val finding = try {
            instance.probeConnection()
            null
        } catch (e: OctoPrintHttpsException) {
            Finding.HttpsNotTrusted(
                certificates = e.serverCertificates,
                weakHostnameVerificationRequired = e.weakHostnameVerificationRequired
            )
        } catch (e: BasicAuthRequiredException) {
            Finding.BasicAuthRequired(e.userRealm)
        } catch (e: Exception) {
            Finding.InvalidConfiguration(e)
        }

        return finding?.let { listOf(it) } ?: emptyList()
    }

    data class Params(
        val webUrl: String
    )

    sealed class Finding {
        data class BasicAuthRequired(
            val userRealm: String
        ) : Finding()

        data class HttpsNotTrusted(
            val certificates: List<Certificate>,
            val weakHostnameVerificationRequired: Boolean,
        ) : Finding()

        data class InvalidConfiguration(
            val exception: Throwable
        ) : Finding()
    }
}