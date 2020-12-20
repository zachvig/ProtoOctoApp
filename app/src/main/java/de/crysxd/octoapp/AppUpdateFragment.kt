package de.crysxd.octoapp

import android.os.Bundle
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import de.crysxd.octoapp.base.OctoAnalytics
import timber.log.Timber

class AppUpdateFragment : Fragment() {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(requireContext()) }
    private val minUpdatePriority = 2
    private val appUpdateType = AppUpdateType.IMMEDIATE
    private val appUpdateRequestCode = 135

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = View(inflater.context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = try {
            appUpdateManager.appUpdateInfo
        } catch (e: Exception) {
            Timber.w("Unable to connect to PlayStore")
            null
        }

        // Checks that the platform will allow the specified type of update.
        Timber.i("Requesting app update info")
        appUpdateInfoTask?.addOnCompleteListener { result ->
            Timber.i("App update info: $result")

            if (result.exception != null) {
                if ((result.exception as? InstallException)?.errorCode != InstallErrorCode.ERROR_APP_NOT_OWNED && result.exception !is RemoteException) {
                    Timber.e(result.exception)
                } else {
                    Timber.w(result.exception)
                }
            } else {
                val appUpdateInfo = result.result

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    OctoAnalytics.logEvent(OctoAnalytics.Event.AppUpdateAvailable)
                    Timber.i("App update info: $appUpdateInfo")

                    if (appUpdateInfo.updatePriority() >= minUpdatePriority && appUpdateInfo.isUpdateTypeAllowed(appUpdateType)) activity?.let {
                        Timber.i("Requesting app update")
                        OctoAnalytics.logEvent(OctoAnalytics.Event.AppUpdatePresented)

                        // Request the update
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            appUpdateType,
                            it,
                            appUpdateRequestCode
                        )
                    }
                }
            }
        }
    }
}