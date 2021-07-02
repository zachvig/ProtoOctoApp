package de.crysxd.octoapp.base

import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import timber.log.Timber

class InvalidApiKeyInterceptor(
    private val octoPrintRepository: OctoPrintRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain) = try {
        chain.proceed(chain.request())
    } catch (e: InvalidApiKeyException) {
        runBlocking {
            Timber.e("API key invalid, clearing data")
            octoPrintRepository.reportActiveApiKeyInvalid()
        }

        throw e
    }
}