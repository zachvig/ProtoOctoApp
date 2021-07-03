package de.crysxd.octoapp.base

import de.crysxd.octoapp.base.models.ActiveInstanceIssue
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor

class DetectBrokenSetupInterceptor(
    private val octoPrintRepository: OctoPrintRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain) = try {
        chain.proceed(chain.request())
    } catch (e: InvalidApiKeyException) {
        runBlocking {
            octoPrintRepository.reportIssueWithActiveInstance(ActiveInstanceIssue.INVALID_API_KEY)
            throw e
        }
    } catch (e: BasicAuthRequiredException) {
        runBlocking {
            octoPrintRepository.reportIssueWithActiveInstance(ActiveInstanceIssue.BASIC_AUTH_REQUIRED)
            throw e
        }
    } catch (e: OctoPrintHttpsException) {
        runBlocking {
            octoPrintRepository.reportIssueWithActiveInstance(ActiveInstanceIssue.HTTP_ISSUE)
            throw e
        }
    }
}