package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.octoprint.UPNP_ADDRESS_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject

class OpenOctoprintWebUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val localDnsResolver: LocalDnsResolver,
    private val context: Context
) : UseCase<OpenOctoprintWebUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        (param.octoPrintWebUrl ?: octoPrintProvider.octoPrint().webUrl).let { webUrl ->
            val host = webUrl.host
            val resolvedUrl = if (host.startsWith(UPNP_ADDRESS_PREFIX) || host.endsWith(".local")) {
                val resolvedHost = withContext(Dispatchers.IO) { localDnsResolver.lookup(host).first() }
                webUrl.newBuilder().host(resolvedHost.hostAddress).build()
            } else {
                webUrl
            }
            val uri = Uri.parse(resolvedUrl.toString())
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    data class Params(
        val octoPrintWebUrl: HttpUrl? = null
    )
}