package de.crysxd.octoapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import okhttp3.logging.HttpLoggingInterceptor

class OctoPrintProvider(
    private val httpLoggingInterceptor: HttpLoggingInterceptor,
    octoPrintRepository: OctoPrintRepository
) {

    val octoPrint: LiveData<OctoPrint?> =
        Transformations.map(octoPrintRepository.instanceInformation) {
            if (it == null) {
                null
            } else {
                createAdHocOctoPrint(it)
            }
        }

    fun createAdHocOctoPrint(it: OctoPrintInstanceInformation) =
        OctoPrint(it.hostName, it.port, it.apiKey, listOf(httpLoggingInterceptor))
}