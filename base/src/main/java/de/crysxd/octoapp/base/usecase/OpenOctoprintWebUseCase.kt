package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.network.LocalDnsResolver
import de.crysxd.octoapp.base.network.OctoPrintUpnpDiscovery.Companion.UPNP_ADDRESS_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class OpenOctoprintWebUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val localDnsResolver: LocalDnsResolver,
) : UseCase<OpenOctoprintWebUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        (param.octoPrintWebUrl ?: octoPrintProvider.octoPrint().webUrl).let { webUrl ->
            val uri = Uri.parse(webUrl)
            val host = uri.host ?: throw IllegalArgumentException("No host in $uri")
            val resolvedUrl = if (host.startsWith(UPNP_ADDRESS_PREFIX)) {
                val resolvedHost = withContext(Dispatchers.IO) { localDnsResolver.lookup(host).first() }
                uri.buildUpon().authority(resolvedHost.hostAddress).build()
            } else {
                uri
            }
            param.context.startActivity(Intent(Intent.ACTION_VIEW, resolvedUrl).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    data class Params(
        val context: Context,
        val octoPrintWebUrl: String? = null
    )
}