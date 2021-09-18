package de.crysxd.octoapp.base.network

import de.crysxd.octoapp.base.models.ActiveInstanceIssue
import de.crysxd.octoapp.base.models.ActiveInstanceIssue.BASIC_AUTH_REQUIRED
import de.crysxd.octoapp.base.models.ActiveInstanceIssue.BASIC_AUTH_REQUIRED_FOR_ALTERNATIVE
import de.crysxd.octoapp.base.models.ActiveInstanceIssue.HTTP_ISSUE
import de.crysxd.octoapp.base.models.ActiveInstanceIssue.HTTP_ISSUE_FOR_ALTERNATIVE
import de.crysxd.octoapp.base.models.ActiveInstanceIssue.INVALID_API_KEY
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.Interceptor
import timber.log.Timber

class DetectBrokenSetupInterceptor(
    private val octoPrintRepository: OctoPrintRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain) = try {
        chain.proceed(chain.request())
    } catch (e: InvalidApiKeyException) {
        Timber.w("Caught InvalidApiKeyException, setup broken (${e.webUrl})")
        reportIssue(e.webUrl, webUrlIssue = INVALID_API_KEY, alternativeWebUrlIssue = INVALID_API_KEY)
        throw e
    } catch (e: BasicAuthRequiredException) {
        Timber.w("Caught BasicAuthRequiredException, setup broken (${e.webUrl})")
        reportIssue(e.webUrl, webUrlIssue = BASIC_AUTH_REQUIRED, alternativeWebUrlIssue = BASIC_AUTH_REQUIRED_FOR_ALTERNATIVE)
        throw e
    } catch (e: OctoPrintHttpsException) {
        Timber.w("Caught OctoPrintHttpsException, setup broken (${e.webUrl})")
        reportIssue(e.webUrl, webUrlIssue = HTTP_ISSUE, alternativeWebUrlIssue = HTTP_ISSUE_FOR_ALTERNATIVE)
        throw e
    }

    private fun reportIssue(url: HttpUrl, webUrlIssue: ActiveInstanceIssue, alternativeWebUrlIssue: ActiveInstanceIssue) {
        AppScope.launch {
            octoPrintRepository.findInstances(url).forEach { res ->
                octoPrintRepository.update(res.first.id) {
                    val issue = if (res.second) alternativeWebUrlIssue else webUrlIssue
                    Timber.w("Reporting $issue")
                    it.copy(issue = issue)
                }
            }
        }
    }
}