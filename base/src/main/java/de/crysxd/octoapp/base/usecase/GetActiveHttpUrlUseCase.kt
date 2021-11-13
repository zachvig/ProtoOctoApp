package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject

class GetActiveHttpUrlUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
) : UseCase<OctoPrintInstanceInformationV3?, GetActiveHttpUrlUseCase.Result>() {

    override suspend fun doExecute(param: OctoPrintInstanceInformationV3?, timber: Timber.Tree): Result {
        // This is a little complicated on first glance, but not that bad
        //
        // Case A: A instance information is given via params (used for widgets) and we need to create an ad hoc instance
        //         In this case we need to fetch the settings to see whether we have a local or a remote connection. OctoEverywhere also will alter the settings
        //         with updated webcam URLs which we need
        //
        // Case B: No instance information is given in params and we use the "default" OctoPrint. In this case we need to check
        //         if the websocket is connected. If this is the case, we can rely on the isAlternativeUrlBeingUsed being correct, no additional network request
        //         is needed to test out the route
        val (octoPrint, settings) = param?.let {
            val o = octoPrintProvider.createAdHocOctoPrint(param)
            val s = o.createSettingsApi().getSettings()
            o to s
        } ?: let {
            val o = octoPrintProvider.octoPrint()
            val connection = octoPrintProvider.passiveConnectionEventFlow("webcam-settings").firstOrNull()?.connectionType
            val s = octoPrintRepository.getActiveInstanceSnapshot()?.settings?.takeIf { connection != null } ?: o.createSettingsApi().getSettings()
            timber.i("Using default connection (connection=$connection)")
            o to s
        }

        val activeUrl = octoPrint.activeUrl
        timber.i("Using default connection (activeUrl=${activeUrl.value})")
        return Result(
            activeUrl = activeUrl,
            settings = settings,
            octoPrint = octoPrint
        )
    }

    data class Result(
        val octoPrint: OctoPrint,
        val settings: Settings,
        val activeUrl: Flow<HttpUrl>
    )
}