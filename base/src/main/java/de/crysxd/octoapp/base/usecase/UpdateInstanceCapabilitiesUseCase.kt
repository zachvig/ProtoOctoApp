package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.os.Build
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.suspendedAwait
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.exceptions.MissingPermissionException
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.companion.AppRegistrationBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

class UpdateInstanceCapabilitiesUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPreferences: OctoPreferences,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
    private val context: Context,
) : UseCase<UpdateInstanceCapabilitiesUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        withContext(Dispatchers.IO) {
            val activeInstance = octoPrintRepository.getActiveInstanceSnapshot() ?: return@withContext
            val state = octoPrintProvider.passiveCurrentMessageFlow("UpdateInstanceCapabilitiesUseCase").firstOrNull()?.state
            val octoPrint = try {
                octoPrintProvider.octoPrint()
            } catch (e: IllegalStateException) {
                timber.w("Cancelling update, no OctoPrint available")
                return@withContext
            }

            if (!activeInstance.isForWebUrl(octoPrint.webUrl)) {
                timber.e("OctoPrint does not match active instance!")
                return@withContext
            }

            // Perform online check. This will trigger switching to the primary web url
            // if we currently use a cloud/backup connection
            if (activeInstance.alternativeWebUrl != null) {
                timber.i("Checking for primary web url being online")
                octoPrint.performOnlineCheck()
            }

            // Gather all info in parallel
            val settings = async {
                octoPrint.createSettingsApi().getSettings()
            }
            val commands = async {
                try {
                    octoPrint.createSystemApi().getSystemCommands()
                } catch (e: MissingPermissionException) {
                    Timber.w("Missing SYSTEM permission")
                    null
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
            val profile = async {
                try {
                    val profiles = octoPrint.createPrinterProfileApi().getPrinterProfiles().profiles.values
                    profiles.firstOrNull { it.current } ?: profiles.firstOrNull { it.default }
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
            val systemInfo = async {
                try {
                    octoPrint.createSystemApi().getSystemInfo()
                } catch (e: MissingPermissionException) {
                    Timber.w("Missing SYSTEM permission")
                    null
                } catch (e: java.lang.Exception) {
                    Timber.e(e)
                    null
                }
            }

            val settingsResult = settings.await()

            val m115Response = if (settingsResult.isCompanionInstalled()) {
                octoPrint.createOctoAppCompanionApi().getFirmwareInfo()
            } else {
                null
            }

            val commandsResult = commands.await()?.all
            val profileResult = profile.await()
            val systemInfoResult = systemInfo.await()

            // Only start update after all network requests are done to prevent race conditions
            octoPrintRepository.update(activeInstance.id) { current ->
                val updated = current.copy(
                    m115Response = m115Response ?: current.m115Response,
                    settings = settingsResult,
                    systemInfo = systemInfoResult ?: current.systemInfo,
                    activeProfile = profileResult ?: current.activeProfile,
                    systemCommands = commandsResult ?: current.systemCommands,
                )
                val standardPlugins = Firebase.remoteConfig.getString("default_plugins").split(",").map { it.trim() }
                settingsResult.plugins.keys.filter { !standardPlugins.contains(it) }.forEach {
                    OctoAnalytics.logEvent(OctoAnalytics.Event.PluginDetected(it))
                }

                timber.i("Updated capabilities: $updated")
                updated
            }

            // Register with companion
            registerWithCompanionPlugin(timber, settingsResult, activeInstance.id, octoPrint)

            // As a second round, we will check the M115 status, but only after a delay to prevent interference with other things
            val isPrinting = state?.flags?.isPrinting() != false
            val isRequired = BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW)
            val isSuppressed = octoPreferences.suppressM115Request
            val isRequested = param.updateM115
            val isCached = activeInstance.m115Response != null || m115Response != null
            if (isCached || isRequested || isPrinting || isRequired || isSuppressed) {
                Timber.i("Skipping M115: isCached=$isCached isPrinting=$isPrinting isRequired=$isRequired isSuppressed=$isSuppressed isRequested=$isRequested")
                return@withContext
            }

            // Execute
            val m115 = try {
                timber.i("Will trigger M115 in 10s")
                delay(10_000)
                executeM115()
            } catch (e: Exception) {
                Timber.e(e)
                null
            }

            // Update
            octoPrintRepository.update(activeInstance.id) { current ->
                timber.i("Storing M115 response")
                current.copy(m115Response = m115 ?: current.m115Response)
            }
        }
    }

    private fun Settings.isCompanionInstalled() = plugins.values.any { it is Settings.OctoAppCompanionSettings }


    private suspend fun registerWithCompanionPlugin(timber: Timber.Tree, settings: Settings, instanceId: String, octoPrint: OctoPrint) {
        try {
            if (settings.isCompanionInstalled()) {
                timber.i("Companion is installed, registering...")
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                octoPrint.createOctoAppCompanionApi().registerApp(
                    AppRegistrationBody(
                        fcmToken = FirebaseMessaging.getInstance().token.suspendedAwait(),
                        displayName = "${Build.BRAND.replaceFirstChar { it.uppercase() }} ${Build.MODEL.replaceFirstChar { it.uppercase() }}",
                        model = Build.MODEL,
                        instanceId = instanceId,
                        appVersion = packageInfo.versionName,
                        appLanguage = BaseInjector.get().getAppLanguageUseCase().execute().appLanguageLocale?.language ?: "en",
                        appBuild = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            packageInfo.versionCode.toLong()
                        }
                    )
                )
            } else {
                timber.i("Companion is not installed")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun executeM115() = try {
        withTimeout(5000L) {
            executeGcodeCommandUseCase.execute(
                ExecuteGcodeCommandUseCase.Param(
                    GcodeCommand.Single("M115"),
                    recordResponse = true,
                    fromUser = false
                )
            )
        }.let {
            val response = it.firstOrNull() as? ExecuteGcodeCommandUseCase.Response.RecordedResponse
            response?.responseLines?.joinToString("\n")
        }
    } catch (e: Exception) {
        Timber.e(e)
        // We do not escalate this error. Fallback to empty.
        null
    }

    data class Params(
        val updateM115: Boolean = true
    )
}