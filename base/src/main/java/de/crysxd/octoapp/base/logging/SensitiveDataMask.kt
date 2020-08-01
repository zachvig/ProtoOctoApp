package de.crysxd.octoapp.base.logging

import de.crysxd.octoapp.base.repository.OctoPrintRepository
import java.util.concurrent.ConcurrentHashMap

class SensitiveDataMask(
    octoPrintRepository: OctoPrintRepository
) {

    private var sensitiveData = ConcurrentHashMap<String, String?>()

    init {
        octoPrintRepository.instanceInformation.observeForever {
            sensitiveData["api_key"] = it?.apiKey
        }
    }


    suspend fun mask(input: String): String {
        var output = input

        sensitiveData.forEach {
            it.value?.let { value ->
                output = output.replace(value, "***${it.key}***")
            }
        }

        return output
    }
}