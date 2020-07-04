package de.crysxd.octoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

class AppUpdateFragment : Fragment() {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(context) }
    private val clientVersionStalenessDays = 5
    private val minUpdatePriority = 2
    private val appUpdateType = AppUpdateType.IMMEDIATE
    private val appUpdateRequestCode = 135

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = View(inflater.context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        Timber.i("Requesting app update info")
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            Timber.i("App update info: $appUpdateInfo")

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.clientVersionStalenessDays() != null &&
                appUpdateInfo.clientVersionStalenessDays() >= clientVersionStalenessDays &&
                appUpdateInfo.updatePriority() >= minUpdatePriority &&
                appUpdateInfo.isUpdateTypeAllowed(appUpdateType)
            ) activity?.let {
                Timber.i("Requesting app update")

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