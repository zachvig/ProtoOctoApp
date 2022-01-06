package de.crysxd.octoapp.base.network

import android.content.Context
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.utils.ExceptionReceivers
import de.crysxd.octoapp.octoprint.exceptions.BasicAuthRequiredException
import de.crysxd.octoapp.octoprint.exceptions.InvalidApiKeyException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintHttpsException
import de.crysxd.octoapp.octoprint.exceptions.WebSocketUpgradeFailedException
import okhttp3.Interceptor

class DetectBrokenSetupInterceptor(
    private val context: Context,
    private val octoPrintRepository: OctoPrintRepository,
) : Interceptor {

    companion object {
        var enabled = true
    }

    override fun intercept(chain: Interceptor.Chain) = try {
        chain.proceed(chain.request())
    } catch (e: Throwable) {
        handleException(e)
        throw e
    }

    private fun isBrokenSetup(e: Throwable) =
        e is BasicAuthRequiredException || e is OctoPrintHttpsException || e is WebSocketUpgradeFailedException || e is InvalidApiKeyException

    fun handleException(e: Throwable) {
        if (enabled && e is OctoPrintException && isBrokenSetup(e)) {
            val active = octoPrintRepository.getActiveInstanceSnapshot() ?: return
            val isForPrimary = active.isForPrimaryWebUrl(e.webUrl)
            val isForAlternative = active.isForAlternativeWebUrl(e.webUrl)

            if (isForAlternative || isForPrimary) {
                ExceptionReceivers.dispatchException(
                    BrokenSetupException(
                        original = e,
                        instance = active,
                        isForPrimary = isForPrimary,
                        userMessage = when (e) {
                            is OctoPrintHttpsException -> context.getString(R.string.sign_in___broken_setup___https_issue, e.originalCause?.message ?: e.message)
                            is BasicAuthRequiredException -> context.getString(R.string.sign_in___broken_setup___basic_auth_required)
                            is InvalidApiKeyException -> context.getString(R.string.sign_in___broken_setup___api_key_revoked)
                            is WebSocketUpgradeFailedException -> context.getString(R.string.sign_in___broken_setup___websocket_upgrade_failed)
                            else -> e.userFacingMessage
                        }
                    )
                )
            }
        }
    }
}