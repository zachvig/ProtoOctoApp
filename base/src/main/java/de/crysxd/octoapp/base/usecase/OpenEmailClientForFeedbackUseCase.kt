package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.os.ConfigurationCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.octoprint.forLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject


class OpenEmailClientForFeedbackUseCase @Inject constructor(
    private val octoPrint: OctoPrintProvider,
    private val getAppLanguageUseCase: GetAppLanguageUseCase
) : UseCase<OpenEmailClientForFeedbackUseCase.Params, Unit>() {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun doExecute(param: Params, timber: Timber.Tree) = withContext(Dispatchers.IO) {
        val context = param.context
        val gson = GsonBuilder().setPrettyPrinting().create()
        val appLanguage = getAppLanguageUseCase.execute(Unit).appLanguageLocale?.language

        val octoPrintVersion = if (param.sendOctoPrintInfo) {
            try {
                octoPrint.octoPrint().createVersionApi().getVersion().serverVersionText
            } catch (e: Exception) {
                Timber.w(e)
                "error"
            }
        } else {
            ""
        }

        val fcmToken = try {
            Tasks.await(FirebaseMessaging.getInstance().token)
        } catch (e: Exception) {
            Timber.w(e)
            "error"
        }

        val pluginList = if (param.sendOctoPrintInfo) {
            try {
                octoPrint.octoPrint().createSettingsApi().getSettings().plugins.map { it.key }
            } catch (e: Exception) {
                Timber.w(e)
                emptyList()
            }
        } else {
            emptyList()
        }

        val email = Firebase.remoteConfig.getString("contact_email")
        val (version, versionCode) = context.packageManager.getPackageInfo(context.packageName, 0).let {
            @Suppress("DEPRECATION")
            Pair(it.versionName, it.versionCode)
        }
        val subject = "Feedback OctoApp $version"

        val publicDir = File(context.externalCacheDir, context.getString(R.string.public_file_dir_name))
        publicDir.mkdir()

        val zipFile = File(publicDir, "data.zip")
        val zipStream = ZipOutputStream(zipFile.outputStream())
        val zipFileUri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", zipFile)
        var fileCount = 0

        if (param.sendLogs) {
            zipStream.putNextEntry(ZipEntry("logs.log"))
            zipStream.writer().apply {
                write(Injector.get().timberCacheTree().logs)
                flush()
            }
            zipStream.closeEntry()
            fileCount++
        }

        if (param.sendPhoneInfo) {
            zipStream.putNextEntry(ZipEntry("phone_info.json"))
            val build = JsonObject()
            build.addProperty("brand", Build.BRAND)
            build.addProperty("manufacturer", Build.MANUFACTURER)
            build.addProperty("languages", ConfigurationCompat.getLocales(Resources.getSystem().configuration).toLanguageTags())
            build.addProperty("model", Build.MODEL)
            build.addProperty("product", Build.PRODUCT)
            build.addProperty("display", Build.DISPLAY)
            build.addProperty("supported_abis", Build.SUPPORTED_ABIS.joinToString())
            build.addProperty("supported_abis_32_bit", Build.SUPPORTED_32_BIT_ABIS.joinToString())
            build.addProperty("supported_abis_64_bit", Build.SUPPORTED_64_BIT_ABIS.joinToString())
            val buildVersion = JsonObject()
            build.addProperty("sdk", Build.VERSION.SDK_INT)
            build.addProperty("release", Build.VERSION.RELEASE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                build.addProperty("preview_sdk", Build.VERSION.PREVIEW_SDK_INT)
                build.addProperty("security_patch", Build.VERSION.SECURITY_PATCH)
            }
            build.add("version", buildVersion)
            val appVersion = JsonObject()
            appVersion.addProperty("version_name", version)
            appVersion.addProperty("version_code", versionCode)
            appVersion.addProperty("app_language", appLanguage)
            appVersion.addProperty("appid", Firebase.auth.currentUser?.uid ?: "")
            appVersion.addProperty("fcmtoken", fcmToken)
            val locale = JsonObject()
            locale.addProperty("language", Locale.getDefault().isO3Language)
            locale.addProperty("country", Locale.getDefault().isO3Country)
            val phoneInfo = JsonObject()
            phoneInfo.add("phone", build)
            phoneInfo.add("app", appVersion)
            phoneInfo.add("locale", locale)

            zipStream.writer().apply {
                write(gson.toJson(phoneInfo))
                flush()
            }
            zipStream.closeEntry()
            fileCount++
        }

        if (param.sendOctoPrintInfo) {
            zipStream.putNextEntry(ZipEntry("octoprint_info.json"))
            zipStream.writer().apply {
                val info = Injector.get().octorPrintRepository().instanceInformationFlow().firstOrNull()
                val json = Gson().toJsonTree(
                    info?.copy(
                        apiKey = "***",
                        webUrl = info.webUrl.forLogging(),
                        alternativeWebUrl = info.alternativeWebUrl?.forLogging(),
                        octoEverywhereConnection = info.octoEverywhereConnection?.copy(
                            connectionId = "***",
                            apiToken = "***",
                            bearerToken = "***",
                            basicAuthPassword = "***",
                            basicAuthUser = "***",
                            fullUrl = info.octoEverywhereConnection.fullUrl.forLogging(),
                            url = info.octoEverywhereConnection.url.forLogging(),
                        )
                    )
                ) as? JsonObject ?: JsonObject()
                json.addProperty("octoprint_version", octoPrintVersion)
                json.add("installed_plugins", Gson().toJsonTree(pluginList))
                write(gson.toJson(json))
                flush()
            }
            zipStream.closeEntry()
            fileCount++
        }

        param.screenshot?.let {
            zipStream.putNextEntry(ZipEntry("screenshot.webp"))
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("Deprecation")
                Bitmap.CompressFormat.WEBP
            }
            it.compress(format, 75, zipStream)
            zipStream.closeEntry()
            fileCount++
        }

        BillingManager.billingFlow().firstOrNull()?.let {
            zipStream.putNextEntry(ZipEntry("billing.json"))
            zipStream.writer().apply {
                write(gson.toJson(it))
                flush()
            }
            zipStream.closeEntry()
        }

        zipStream.close()

        val intent = Intent(Intent.ACTION_SEND).also {
            it.type = "message/rfc822"
            it.putExtra(Intent.EXTRA_SUBJECT, subject)
            it.putExtra(Intent.EXTRA_TEXT, param.message)
            it.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))

            if (fileCount > 0) {
                it.putExtra(Intent.EXTRA_STREAM, zipFileUri)
            }
        }

        context.startActivity(intent)
    }

    data class Params(
        val context: Context,
        val sendPhoneInfo: Boolean,
        val sendLogs: Boolean,
        val sendOctoPrintInfo: Boolean,
        val screenshot: Bitmap?,
        val message: String
    )
}