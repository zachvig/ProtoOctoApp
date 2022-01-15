package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.network.OctoPrintProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject

class GetActiveHttpUrlUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<OctoPrintInstanceInformationV3?, Flow<HttpUrl>>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV3?, timber: Timber.Tree): Flow<HttpUrl> {
        val octoPrint = param?.let {
            octoPrintProvider.octoPrintFlow(param.id)
        } ?: let {
            octoPrintProvider.octoPrintFlow()
        }

        return octoPrint.flatMapLatest {
            it ?: return@flatMapLatest emptyFlow()
            // Probe connection to ensure primary or alternative URL is active
            if (octoPrintProvider.getCurrentConnection(it.id) == null) {
                it.createVersionApi().getVersion()
                timber.d("OctoPrint not connected, probing connection")
            } else {
                timber.d("OctoPrint connected, relying on active web url")
            }
            it.activeUrl
        }
    }
}