package de.crysxd.octoapp.base.usecase

import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.plugins.materialmanager.Material
import timber.log.Timber
import javax.inject.Inject

class GetMaterialsUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository
) : UseCase<Unit, List<Material>>() {

    override suspend fun doExecute(param: Unit, timber: Timber.Tree): List<Material> {
        val octoPrint = octoPrintProvider.octoPrint()
        val settings = octoPrintRepository.getActiveInstanceSnapshot()?.settings
            ?: octoPrint.createSettingsApi().getSettings()
        val materials = octoPrint.createMaterialManagerPluginsCollection().getMaterials(settings)

        // Eliminate duplicate names by adding vendor
        val duplicateNames = mutableSetOf<String>()
        val duplicateNameResolutions = mutableMapOf<String, NameConflictResolution>()
        materials.forEach { m ->
            // Already identified as duplicate, skip
            if (duplicateNames.contains(m.displayName)) return@forEach

            val sameNames = materials.filter { it.displayName == m.displayName }
            if (sameNames.size == 1) return@forEach

            // Alter names to append material
            val materialNames = sameNames.map { it.displayName + it.material }
            val vendorNames = sameNames.map { it.displayName + it.vendor }
            val materialVendorNames = sameNames.map { it.displayName + it.vendor + it.material }
            val materialNamesAreDistinct = materialNames.distinct().size == sameNames.size
            val vendorNamesAreDistinct = vendorNames.distinct().size == sameNames.size
            val materialVendorNamesAreDistinct = materialVendorNames.distinct().size == sameNames.size
            duplicateNameResolutions[m.displayName] = when {
                materialNamesAreDistinct -> NameConflictResolution.AddMaterial
                vendorNamesAreDistinct -> NameConflictResolution.AddVendor
                materialVendorNamesAreDistinct -> NameConflictResolution.AddMaterialAndVendor
                else -> NameConflictResolution.AddMaterialAndVendorAndId
            }
        }

        // Copy materials and alter names if required
        return materials.map {
            when (duplicateNameResolutions[it.displayName]) {
                NameConflictResolution.AddMaterial -> it.copy(displayName = "${it.displayName} (${it.material})")
                NameConflictResolution.AddVendor -> it.copy(displayName = "${it.displayName} (${it.vendor})")
                NameConflictResolution.AddMaterialAndVendor -> it.copy(displayName = "${it.displayName} (${it.material}, ${it.vendor})")
                NameConflictResolution.AddMaterialAndVendorAndId -> it.copy(displayName = "${it.displayName} (${it.material}, ${it.vendor}, ${it.id})")
                null -> it
            }
        }
    }

    private sealed class NameConflictResolution {
        object AddVendor : NameConflictResolution()
        object AddMaterial : NameConflictResolution()
        object AddMaterialAndVendor : NameConflictResolution()
        object AddMaterialAndVendorAndId : NameConflictResolution()
    }
}