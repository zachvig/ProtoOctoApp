package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import timber.log.Timber
import javax.inject.Inject

class GetCurrentPrinterProfileUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<Unit, PrinterProfiles.Profile>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): PrinterProfiles.Profile {
        val response = octoPrintProvider.octoPrint().createPrinterProfileApi().getPrinterProfiles()
        return response.profiles.values.firstOrNull { it.current }
            ?: response.profiles.values.firstOrNull { it.default }
            ?: PrinterProfiles.Profile(
                volume = PrinterProfiles.Volume(200f, 200f, 200f, PrinterProfiles.Origin.LowerLeft),
                current = false,
                default = false,
                extruder = PrinterProfiles.Extruder(0.4f),
                model = "fallback",
                name = "Fallback",
            )
    }
}