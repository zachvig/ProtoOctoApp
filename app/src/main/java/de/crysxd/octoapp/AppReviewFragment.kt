package de.crysxd.octoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import de.crysxd.octoapp.base.di.Injector
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AppReviewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = View(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialise conditions, will set first start if not present
        val loaded = loadConditions()
        storeConditions(loaded.copy(appLaunchCounter = loaded.appLaunchCounter + 1))

        lifecycleScope.launchWhenResumed {
            Timber.d("Installing review request trigger")
            requireActivity().findNavController(R.id.mainNavController).addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.printControlsFragment) {
                    storeConditions(loadConditions().copy(printWasActive = true))
                }
            }
        }
    }

    private fun loadConditions() = Injector.get().sharedPreferences().getString(KEY_CONDITIONS, null)?.let {
        Gson().fromJson(it, ReviewFlowConditions::class.java)
    } ?: ReviewFlowConditions()

    private fun storeConditions(conditions: ReviewFlowConditions) {
        Injector.get().sharedPreferences().edit {
            putString(KEY_CONDITIONS, Gson().toJson(conditions))
        }

        checkConditionsMetAndLaunch()
    }

    private fun checkConditionsMetAndLaunch() {
        val config = Firebase.remoteConfig
        config.ensureInitialized().addOnSuccessListener {
            val conditions = loadConditions()
            val minAppLaunches = config.getLong("review_flow_condition_min_app_launches")
            val minAppUsageMinutes = config.getLong("review_flow_condition_min_app_usage_min")
            val printRequired = config.getBoolean("review_flow_condition_print_was_active_required")
            Timber.d("Checking conditions: $conditions <--> minAppLaunches=$minAppLaunches, minAppUsageMinutes=$minAppUsageMinutes, printRequired=$printRequired")

            val appLaunchesOk = conditions.appLaunchCounter >= minAppLaunches
            val appUsageOk = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - conditions.firstStart) >= minAppUsageMinutes
            val printStateOk = (conditions.printWasActive || !printRequired)

            if (listOf(appLaunchesOk, appUsageOk, printStateOk).all { it }) {
                Timber.d("Conditions met")
                launchReviewFlow()
            } else {
                Timber.d("Conditions not met")
            }
        }
    }

    private fun launchReviewFlow() = lifecycleScope.launchWhenResumed {
        Timber.d("Launching review flow")
        val manager = ReviewManagerFactory.create(requireContext())
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener {
            lifecycleScope.launchWhenCreated {
                if (it.isSuccessful) {
                    // We got the ReviewInfo object
                    val reviewInfo = it.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        Timber.d("Review flow completed")
                    }
                } else {
                    Timber.e("Review flow not successful")
                    Timber.e(it.exception)
                }
            }
        }
    }

    companion object {
        const val KEY_CONDITIONS = "app_review_trigger_conditions"
    }

    private data class ReviewFlowConditions(
        val appLaunchCounter: Int = 0,
        val firstStart: Long = System.currentTimeMillis(),
        val printWasActive: Boolean = false
    )
}