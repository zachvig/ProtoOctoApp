package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.network.OctoPrintProvider
import timber.log.Timber
import javax.inject.Inject

class GetInstalledPluginsUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, List<String>>() {
    override suspend fun doExecute(param: Unit, timber: Timber.Tree): List<String> = octoPrintProvider.octoPrint().createPluginManagerApi().getPlugins().plugins
        ?.filter { it.enabled != false }
        ?.map { it.key }
        ?: emptyList()
}