package de.crysxd.octoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class AppUpdateFragment : Fragment() {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(context) }
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
        appUpdateInfoTask.addOnCompleteListener { result ->
            Timber.i("App update info: $result")

            if (result.exception != null) {
                Timber.e(result.exception)
            } else {
                Firebase.analytics.logEvent("app_update_available", Bundle.EMPTY)
                val appUpdateInfo = result.result
                Timber.i("App update info: $appUpdateInfo")

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.updatePriority() >= minUpdatePriority &&
                    appUpdateInfo.isUpdateTypeAllowed(appUpdateType)
                ) activity?.let {
                    Timber.i("Requesting app update")
                    FirebaseAnalytics.getInstance(requireContext()).logEvent("app_update_presented", Bundle.EMPTY)

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