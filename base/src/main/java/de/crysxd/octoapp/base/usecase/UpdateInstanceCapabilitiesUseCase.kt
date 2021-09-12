package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.os.Build
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.OctoPreferences
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.suspendedAwait
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.OctoPrint
import de.crysxd.octoapp.octoprint.exceptions.MissingPermissionException
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.octoprint.plugins.companion.AppRegistrationBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

class UpdateInstanceCapabilitiesUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPreferences: OctoPreferences,
    private val executeGcodeCommandUseCase: ExecuteGcodeCommandUseCase,
    private val getCurrentPrinterProfileUseCase: GetCurrentPrinterProfileUseCase,
    private val context: Context,
) : UseCase<UpdateInstanceCapabilitiesUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        withContext(Dispatchers.IO) {
            val activeInstance = octoPrintRepository.getActiveInstanceSnapshot() ?: return@withContext
            val octoPrint = try {
                octoPrintProvider.octoPrint()
            } catch (e: IllegalStateException) {
                timber.w("Cancelling update, no OctoPrint available")
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
                val settings = octoPrint.createSettingsApi().getSettings()
                registerWithCompanionPlugin(timber, settings, activeInstance.id, octoPrint)
                settings
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
                    getCurrentPrinterProfileUseCase.execute(Unit)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
            val m115 = async {
                try {
                    // Don't execute M115 if we suppress it manually (might cause issues on some machines) or if we don't use the Gcode preview (as this is where we use it)
                    if (param.updateM115 && BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW) && !octoPreferences.suppressM115Request) {
                        executeM115()
                    } else {
                        Timber.i("Skipping M115")
                        null
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }

            val m115Result = m115.await()
            val settingsResult = settings.await()
            val commandsResult = commands.await()?.all
            val profileResult = profile.await()

            // Only start update after all network requests are done to prevent race conditions
            octoPrintRepository.update(activeInstance.id) { current ->
                val updated = current.copy(
                    m115Response = m115Result ?: current.m115Response,
                    settings = settingsResult,
                    activeProfile = profileResult ?: current.activeProfile,
                    systemCommands = commandsResult ?: current.systemCommands,
                )
                val standardPlugins = Firebase.remoteConfig.getString("default_plugins").split(",").map { it.trim() }
                settings.await().plugins.keys.filter { !standardPlugins.contains(it) }.forEach {
                    OctoAnalytics.logEvent(OctoAnalytics.Event.PluginDetected(it))
                }

                timber.i("Updated capabilities: $updated")
                updated
            }
        }
    }

    private suspend fun registerWithCompanionPlugin(timber: Timber.Tree, settings: Settings, instanceId: String, octoPrint: OctoPrint) {
        try {
            if (settings.plugins.values.any { it is Settings.OctoAppCompanionSettings }) {
                timber.i("Companion is installed, registering...")
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                octoPrint.createOctoAppCompanionApi().registerApp(
                    AppRegistrationBody(
                        fcmToken = FirebaseMessaging.getInstance().token.suspendedAwait(),
                        displayName = "${Build.BRAND.replaceFirstChar { it.uppercase() }} ${Build.MODEL.replaceFirstChar { it.uppercase() }}",
                        model = Build.MODEL,
                        instanceId = instanceId,
                        appVersion = packageInfo.versionName,
                        appLanguage = Injector.get().getAppLanguageUseCase().execute().appLanguageLocale?.language ?: "en",
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