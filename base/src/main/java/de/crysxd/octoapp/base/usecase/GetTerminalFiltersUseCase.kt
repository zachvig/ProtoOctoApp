package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.settings.Settings
import timber.log.Timber
import javax.inject.Inject

class GetTerminalFiltersUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, List<Settings.TerminalFilter>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree) =
        octoPrintProvider.octoPrint().createSettingsApi().getSettings().terminalFilters

}