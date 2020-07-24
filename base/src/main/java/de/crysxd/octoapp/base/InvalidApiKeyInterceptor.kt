package de.crysxd.octoapp.base

import de.crysxd.octoapp.base.repository.OctoPrintRepository
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class InvalidApiKeyInterceptor(
    private val octoPrintRepository: OctoPrintRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 403) {
            Timber.e("API key invalid, clearing data")
            octoPrintRepository.clearOctoprintInstanceInformation()
        }

        return response
    }
}