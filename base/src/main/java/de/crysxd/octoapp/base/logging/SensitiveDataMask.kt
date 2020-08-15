package de.crysxd.octoapp.base.logging

import de.crysxd.octoapp.base.repository.OctoPrintRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class SensitiveDataMask(
    octoPrintRepository: OctoPrintRepository
) {

    private var sensitiveData = ConcurrentHashMap<String, String?>()

    init {
        GlobalScope.launch {
            supervisorScope {
                octoPrintRepository.instanceInformationFlow()
                    .onEach {
                        Timber.i("Collected $it")
                        val key = "api_key"
                        if (it?.apiKey != null) {
                            sensitiveData[key] = it.apiKey
                        } else {
                            sensitiveData.remove(key)
                        }
                    }
                    .retry { Timber.e(it); delay(100); true }
                    .collect()
            }
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