package de.crysxd.octoapp.base.network

import de.crysxd.octoapp.base.models.ActiveInstanceIssue
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import timber.log.Timber

class DetectBrokenSetupInterceptor(
    private val octoPrintRepository: OctoPrintRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain) = try {
        chain.proceed(chain.request())
    } catch (e: InvalidApiKeyException) {
        Timber.w("Caught InvalidApiKeyException, setup broken")
        runBlocking {
            octoPrintRepository.reportIssueWithActiveInstance(ActiveInstanceIssue.INVALID_API_KEY)
            throw e
        }
    } catch (e: BasicAuthRequiredException) {
        Timber.w("Caught BasicAuthRequiredException, setup broken")
        runBlocking {
            octoPrintRepository.reportIssueWithActiveInstance(ActiveInstanceIssue.BASIC_AUTH_REQUIRED)
            throw e
        }
    } catch (e: OctoPrintHttpsException) {
        Timber.w("Caught OctoPrintHttpsException, setup broken")
        runBlocking {
            octoPrintRepository.reportIssueWithActiveInstance(ActiveInstanceIssue.HTTP_ISSUE)
            throw e
        }
    }
}