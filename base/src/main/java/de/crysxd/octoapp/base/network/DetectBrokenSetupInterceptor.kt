package de.crysxd.octoapp.base.network

import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.BASIC_AUTH_REQUIRED
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.BASIC_AUTH_REQUIRED_FOR_ALTERNATIVE
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.HTTP_ISSUE
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.HTTP_ISSUE_FOR_ALTERNATIVE
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.INVALID_API_KEY
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.WEBSOCKET_UPGRADE_FAILED
import de.crysxd.octoapp.base.data.models.ActiveInstanceIssue.WEBSOCKET_UPGRADE_FAILED_FOR_ALTERNATIVE
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import de.crysxd.octoapp.octoprint.exceptions.WebSocketUpgradeFailedException
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
        handleException(e)
        throw e
    } catch (e: BasicAuthRequiredException) {
        handleException(e)
        throw e
    } catch (e: OctoPrintHttpsException) {
        handleException(e)
        throw e
    }

    fun handleException(e: Throwable) {
        when (e) {
            is InvalidApiKeyException -> {
                Timber.w(e, "Caught InvalidApiKeyException, setup broken (${e.webUrl})")
                reportIssue(e.webUrl, webUrlIssue = INVALID_API_KEY, alternativeWebUrlIssue = INVALID_API_KEY)
            }

            is BasicAuthRequiredException -> {
                Timber.w(e, "Caught BasicAuthRequiredException, setup broken (${e.webUrl})")
                reportIssue(e.webUrl, webUrlIssue = BASIC_AUTH_REQUIRED, alternativeWebUrlIssue = BASIC_AUTH_REQUIRED_FOR_ALTERNATIVE)
            }

            is OctoPrintHttpsException -> {
                Timber.e(e, "Caught OctoPrintHttpsException, setup broken (${e.webUrl})")
                reportIssue(e.webUrl, webUrlIssue = HTTP_ISSUE, alternativeWebUrlIssue = HTTP_ISSUE_FOR_ALTERNATIVE, throwable = e)
            }

            is WebSocketUpgradeFailedException -> {
                Timber.e(e, "Caught WebSocketUpgradeFailedException, setup broken (${e.webUrl})")
                reportIssue(e.webUrl, webUrlIssue = WEBSOCKET_UPGRADE_FAILED, alternativeWebUrlIssue = WEBSOCKET_UPGRADE_FAILED_FOR_ALTERNATIVE, throwable = e)
            }
        }
    }

    private fun reportIssue(url: HttpUrl, webUrlIssue: ActiveInstanceIssue, alternativeWebUrlIssue: ActiveInstanceIssue, throwable: Throwable? = null) {
        AppScope.launch {
            octoPrintRepository.findInstances(url).forEach { res ->
                octoPrintRepository.update(res.first.id) {
                    val issue = if (res.second) alternativeWebUrlIssue else webUrlIssue
                    Timber.w("Reporting $issue from $throwable")
                    it.copy(issue = issue, issueMessage = throwable?.let { t -> "${t::class.java}: ${t.message}" })
                }
            }
        }
    }
}