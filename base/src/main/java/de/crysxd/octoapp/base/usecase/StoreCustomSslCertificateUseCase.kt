package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.SslKeyStoreHandler
import timber.log.Timber
import java.security.cert.Certificate
import javax.inject.Inject

class StoreCustomSslCertificateUseCase @Inject constructor(
    private val sslKeyStoreHandler: SslKeyStoreHandler
) : UseCase<List<Certificate>, Unit>() {

    override suspend fun doExecute(param: List<Certificate>, timber: Timber.Tree) {
        sslKeyStoreHandler.storeCertificates(param)
    }
}